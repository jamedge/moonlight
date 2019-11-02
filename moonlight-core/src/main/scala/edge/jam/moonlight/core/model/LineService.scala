package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.{ElementClass, ElementType, GraphElement}
import neotypes.{DeferredQuery, DeferredQueryBuilder, Driver, Transaction}

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
        constructInputRelationshipsCleaning(line, inputsGenerated).execute(tx)
        line.io.flatMap { io =>
          io.inputs.flatMap { input =>
            constructInputQuery(line, input).execute(tx)
            input.storage.map(constructAttachStorageQuery(line, input, _).execute(tx)).getOrElse(Future())
            val outputsGenerated = GraphElements.foldList(io.outputs.map(el => c"${el.name}"))
            constructOutputRelationshipsCleaning(line, input, outputsGenerated).execute(tx)
            io.outputs.map { output =>
              constructOutputQuery(line, input, output).execute(tx)
              output.storage.map(constructAttachStorageQuery(line, output, _).execute(tx)).getOrElse(Future())
            }
          }
        }
        cleanup(tx)
      }
    }
  }

  private def constructInputRelationshipsCleaning(line:Line, existingInputsGenerated: DeferredQueryBuilder): DeferredQuery[Unit] = {
    constructRelationshipCleaning(line, N.Line(line, "line"), existingInputsGenerated, ElementClass.IO)
  }

  private def constructOutputRelationshipsCleaning(line:Line, input: IOElement, existingOutputsGenerated: DeferredQueryBuilder): DeferredQuery[Unit] = {
    constructRelationshipCleaning(line, N.IO(input, "input"), existingOutputsGenerated, ElementClass.IO)
  }

  private def cleanup(tx: Transaction[Future]): Future[Unit] = {
    constructDeleteCleanedRelationships().execute(tx)
    constructDeleteDetachedNodes(ElementClass.IO).execute(tx)
    constructDeleteDetachedNodes(ElementClass.Storage).execute(tx)
  }

  // TODO: extract all DDL keywords to GraphElements
  // marks IO relationships previously used by line but not provided in version which is passed for deletion by setting their "toDelete" attribute to true and removing line name from attribute "fromLines"
  private def constructRelationshipCleaning(
      line:Line,
      startElement: GraphElement,
      existingConnectingElementsGenerated: DeferredQueryBuilder,
      connectingElementClass: ElementClass
  ): DeferredQuery[Unit] = {
    val query = (c"MATCH" + startElement.toSearchObject() + c"-[ir]->" + s"(i:${connectingElementClass.name})" +
      c"WHERE NOT i.name IN" + existingConnectingElementsGenerated +
      c"WITH i, ir" +
      c"MATCH p = (i) -[r*1..]-> (othersOut) <-[s*0..]- (connection)" +
      c"FOREACH ( x IN r | SET (CASE WHEN " + c"${line.name}" +
      c"IN x.fromLines THEN x END).toDelete = ${"true"})" +
      c"WITH p, ir" +
      c"MATCH (a) -[r {toDelete: ${"true"}}]-> (b)" +
      c"SET r.fromLines = FILTER ( y IN r.fromLines WHERE y <> ${line.name})" +
      c"DELETE ir").query[Unit]
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
  private def constructDeleteDetachedNodes(elementClass: ElementClass): DeferredQuery[Unit] = {
    val query =
      (c"MATCH" + s"(i:${elementClass.name})" + c"WHERE NOT (i) <-- () DELETE i").query[Unit]
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
