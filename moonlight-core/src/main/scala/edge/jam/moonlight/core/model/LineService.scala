package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.{ElementType, GraphElement}
import neotypes.{DeferredQuery, DeferredQueryBuilder, Driver}

import scala.concurrent.{ExecutionContext, Future}
import neotypes.implicits._
import org.slf4j.Logger
import edge.jam.moonlight.core.model.{Nodes => N, Relationships => R}

class LineService(
    neo4jDriver: Driver[Future],
    logger: Logger
)(implicit val executionContext: ExecutionContext) {

  def getAllCodeMetadata(): Future[Option[String]] = {
    val result = neo4jDriver.readSession { session =>
      c"""MATCH (code:Code) RETURN code
       """.query[Option[Code]].single(session)
    }

    result.map(_.map(_.toString))
  }

  def addLine(line: Line): Future[Unit] = {
    neo4jDriver.writeSession { implicit session =>
      session.transact[Unit] { implicit tx =>
        constructLineQuery(line).execute(tx)
        constructLineDetailsQuery(line).execute(tx)
        val inputsGenerated = GraphElements.foldList(line.io.flatMap(_.inputs).map(el => c"${el.name}"))
        constructInputRelationshipCleaning(line, inputsGenerated).execute(tx)
        constructDeleteCleanedRelationships().execute(tx)
        constructDeleteDetachedInputs().execute(tx)
        line.io.flatMap { io =>
          io.inputs.flatMap { input =>
            constructInputQuery(line, input).execute(tx)
            input.storage.map(constructAttachStorageQuery(line, input, _).execute(tx)).getOrElse(Future())
            val outputsGenerated = GraphElements.foldList(io.outputs.map(el => c"${el.name}"))
            constructMutualOutputsQuery(input, outputsGenerated).list(tx).map { foundOutputs =>
              if (foundOutputs.nonEmpty) {
                val foundOutputsGenerated = GraphElements.foldList(foundOutputs.map(el => c"${el.name}"))
                val allOutputsGenerated = GraphElements.foldList((io.outputs ++ foundOutputs).map(el => c"${el.name}"))
                constructDetachOutputs(line, input, foundOutputsGenerated).execute(tx)
                constructDeleteOutputs(line, input, allOutputsGenerated).execute(tx)
              } else {
                constructDeleteOutputs(line, input, outputsGenerated).execute(tx)
              }
            }
            io.outputs.map { output =>
              constructOutputQuery(line, input, output).execute(tx)
              output.storage.map(constructAttachStorageQuery(line, output, _).execute(tx)).getOrElse(Future())
            }
          }
        }
        Future()
      }
    }
  }

  // TODO: extract all DDL keywords to GraphElements
  // finds IO nodes that have relationship with other IO nodes (other then existing one)
  private def constructMutualOutputsQuery(input: IOElement, outputsGenerated: DeferredQueryBuilder): DeferredQuery[IOElement] = {
    val (i, o, others) = (
      N.IO(input, "i"),
      N.IO("o"),
      N.IO("others"))
    val rHasOutputRight = R.HasOutput(elementType = ElementType.RelationshipRight)
    val query = (c"MATCH" + i.toSearchObject() + rHasOutputRight.toAnyObjectOfType() + o.toAnyObjectOfType() + c"--" + others.toAnyObjectOfType() +
      c"WHERE NOT" + o.toVariableWithField("name") + c"IN" + outputsGenerated + c"AND" + others.toVariableWithField("name") + c"<> ${input.name}" +
      c"RETURN" + o.toVariable()).query[IOElement]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  // detaches a relationship with mutual output if it's created by this line and no others or removes line name from the "fromLines" property of the relationship
  private def constructDetachOutputs(line: Line, input: IOElement, foundOutputsGenerated: DeferredQueryBuilder): DeferredQuery[Unit] = {
    val (i, o) = (
      N.IO(input, "i"),
      N.IO("o"))
    val r = R.HasOutput(variablePrefix = "r")
    val query = (c"MATCH" + i.toSearchObject() + r.toAnyObjectOfType() + o.toAnyObjectOfType() +
      c"WHERE" + o.toVariableWithField("name") + c"IN" + foundOutputsGenerated +
      c"AND ${line.name} IN" + r.toVariableWithNewField("fromLines") +
      c"SET" + r.toVariableWithNewField("fromLines") + c"= [x IN" + r.toVariableWithNewField("fromLines") + c" WHERE x <> ${line.name}]" +
      c"WITH" + i.toVariable() + c"MATCH" + i.toVariableEnclosed() + r.toAnyObjectOfType() + o.toAnyObjectOfType() +
      c"WHERE" + o.toVariableWithField("name") + c"IN" + foundOutputsGenerated +
      c"AND" + r.toVariableWithNewField("fromLines") + c"= []" +
      c"DELETE" + r.toVariable()).query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  // deletes an output that has no other connections if it's used only by selected line; otherwise just removes line name from the "fromLines" property of the relationship
  private def constructDeleteOutputs(line: Line, input: IOElement, existingOutputsGenerated: DeferredQueryBuilder): DeferredQuery[Unit] = {
    val (i, o) = (
      N.IO(input, "i"),
      N.IO("o"))
    val r = R.HasOutput(variablePrefix = "r")
    val query = (c"MATCH" + i.toSearchObject() + r.toAnyObjectOfType() + o.toAnyObjectOfType() +
      c"WHERE NOT" + o.toVariableWithField("name") + c"IN" + existingOutputsGenerated +
      c"AND ${line.name} IN" + r.toVariableWithNewField("fromLines") +
      c"SET" + r.toVariableWithNewField("fromLines") + c"= [x IN" + r.toVariableWithNewField("fromLines") + c" WHERE x <> ${line.name}]" +
      c"WITH" + i.toVariable() + c"MATCH" + i.toVariableEnclosed() + r.toAnyObjectOfType() + o.toAnyObjectOfType() +
      c"WHERE NOT" + o.toVariableWithField("name") + c"IN" + existingOutputsGenerated +
      c"AND" + r.toVariableWithNewField("fromLines") + c"= []" +
      c"DELETE" + r.toVariable() + c"," + o.toVariable()).query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  // marks IO relationships previously used by line but not provided in version which is passed for deletion by setting their "toDelete" attribute to true and removing line name from attribute "fromLines"
  private def constructInputRelationshipCleaning(line: Line, existingInputsGenerated: DeferredQueryBuilder): DeferredQuery[Unit] = {
    val l = N.Line(line, "l")
    val ir = R.HasInput(Map(), "ir")
    val i = N.IO("i")
    val r = R.HasOutput(Map(), "r")
    val othersOut = N.IO("othersOut")
    val s = R.HasOutput(Map(), "s", ElementType.RelationshipLeft)
    val connection = N.IO("connection")
    val x = R.HasOutput(Map(), "x")
    val a = N.IO("a")
    val b = N.IO("b")
    val y = R.HasOutput(Map(), "y")
    val query = (c"MATCH" + l.toSearchObject() + ir.toAnyObjectOfType() + i.toAnyObjectOfType() +
      c"WHERE NOT" + i.toVariableWithField("name") +  c"IN" + existingInputsGenerated +
      c"WITH" + i.toVariable() + c"," + ir.toVariable() +
      c"MATCH p =" + i.toVariableEnclosed() + r.toVariableEnclosedWithCardinality(Some(1), None) + othersOut.toVariableEnclosed() +
        s.toVariableEnclosedWithCardinality(Some(0), None) + connection.toVariableEnclosed() +
      c"FOREACH (" + x.toVariable() + "IN" + r.toVariable() + c"| SET (CASE WHEN " + c"${line.name}" +
        c"IN" + x.toVariableWithNewField("fromLines") + c"THEN" + x.toVariable() + c"END).toDelete = ${"true"})" +
      c"WITH p," + ir.toVariable() +
      c"MATCH" + a.toVariableEnclosed() + r.toSearchObjectSpecified("toDelete", "true") + b.toVariableEnclosed() +
      c"SET" + r.toVariableWithNewField("fromLines") +
        c"= FILTER (" + y.toVariable() + c"IN" + r.toVariableWithNewField("fromLines") + c"WHERE" + y.toVariable() + c"<> ${line.name})" +
      c"DELETE" + ir.toVariable()).query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  // deletes all IO relationships with toDelete = true and fromLines = []
  private def constructDeleteCleanedRelationships(): DeferredQuery[Unit] = {
    val i = N.IO("i")
    val r = R.HasOutput(Map(), "s", ElementType.RelationshipLeft)
    val query =
      (c"MATCH" + i.toVariableEnclosed() + r.toSearchObjectSpecified(Map("toDelete" -> "true", "fromLines" -> List())) + c"()" +
        "DELETE" + r.toVariable()).query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  // deletes all detached IO nodes
  private def constructDeleteDetachedInputs(): DeferredQuery[Unit] = {
    val query =
      c"MATCH (i:IO:Storage) WHERE NOT (i) <-- () DELETE i".query[Unit]
    logQueryCreation(query)
    query
  }

  private def constructLineQuery(line: Line): DeferredQuery[Unit] = {
    val lineNode = N.Line(line, "l")
    val query = c""
      .+(GraphElements.constructCreateOrUpdateQuery(lineNode)).query[Unit]
    logQueryCreation(query)
    query
  }

  private def constructLineDetailsQuery(line: Line): DeferredQuery[Unit] = {
    val lineNode = N.Line(line, "l")
    val query = c""
      .+(line.details.map(ld => GraphElements.constructCreateOrUpdateQuery(
        lineNode,
        Some(R.HasDetails()),
        Some(N.Details(ld, "ld")),
        true)).getOrElse(
        GraphElements.constructDeleteQuery(
          lineNode,
          R.HasDetails(Map(), "ldr"),
          N.Details(Map(), "ld")))).query[Unit]
    logQueryCreation(query)
    query
  }

  private def constructInputQuery(line: Line, input: IOElement): DeferredQuery[Unit] = {
    val lineNode = N.Line(line, "l")
    val r = R.HasInput(Map(), "r")
    val query = c""
      .+(GraphElements.constructCreateOrUpdateQuery(
        lineNode,
        Some(r),
        Some(N.IO(input, "i")),
        false,
        None,
        Some(c"ON MATCH SET (CASE WHEN NOT ${line.name} IN" + r.toVariableWithNewField("fromLines") +
          c"THEN" + r.toVariable() + c"END).fromLines =" + r.toVariableWithNewField("fromLines") + c" + [${line.name}]" +
          c"ON CREATE SET" + r.toVariableWithNewField("fromLines") + c"= [${line.name}]"
        )
      )).query[Unit]
    logQueryCreation(query)
    query
  }

  private def constructAttachStorageQuery(line: Line, ioElement: IOElement, storage: Storage): DeferredQuery[Unit] = {
    val io = N.IO(ioElement, "io")
    val s = N.Storage(storage, "s")
    val r = R.HasStorage(Map(), "r")
    val query = c""
      .+(GraphElements.constructCreateOrUpdateQuery(
        io,
        Some(r),
        Some(s),
        false,
        None,
        Some(c"ON MATCH SET (CASE WHEN NOT ${line.name} IN" + r.toVariableWithNewField("fromLines") +
          c"THEN" + r.toVariable() + c"END).fromLines =" + r.toVariableWithNewField("fromLines") + c" + [${line.name}]" +
          c"ON CREATE SET" + r.toVariableWithNewField("fromLines") + c"= [${line.name}]"
        )
      )).query[Unit]
    query
  }

  private def constructOutputQuery(line: Line, input: IOElement, output: IOElement): DeferredQuery[Unit] = {
    val lineNode = N.Line(line, "l")
    val inputNode = N.IO(input, "i")
    val outputNode = N.IO(output, "o")
    val relationship = R.HasOutput(Map(), "r")
    val query = c""
      .+(GraphElements.constructCreateOrUpdateQuery(
        inputNode,
        Some(relationship),
        Some(N.IO(output, "o")),
        false,
        None,
        Some(c"ON MATCH SET (CASE WHEN NOT ${line.name} IN" + relationship.toVariableWithNewField("fromLines") +
          c"THEN" + relationship.toVariable() + c"END).fromLines =" + relationship.toVariableWithNewField("fromLines") + c" + [${line.name}]" +
          c"ON CREATE SET" + relationship.toVariableWithNewField("fromLines") + c"= [${line.name}]"
        )
      )).query[Unit]
    logQueryCreation(query)
    query
  }

  private def logQueryCreation(query: DeferredQuery[_]): Unit = {
    logger.debug(s"Creating query:\n${query.query}\nwith params:\n${query.params.map(p => s"${p._1}: ${p._2.toString}")}")
  }
}
