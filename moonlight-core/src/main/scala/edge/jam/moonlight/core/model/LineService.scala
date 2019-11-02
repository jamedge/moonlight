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
        cleanupDownstream(line, tx)
        constructLineQuery(line).execute(tx)
        constructLineDetailsQuery(line).execute(tx)
        line.io.flatMap { io =>
          io.inputs.flatMap { input =>
            constructInputQuery(line, input).execute(tx)
            input.storage.map(constructAttachStorageQuery(line, input, _).execute(tx)).getOrElse(Future())
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

  private def cleanupDownstream(line:Line, tx: Transaction[Future]): Future[Unit] = {
    constructRelationshipDeleteMarking(line, N.Line(line, "l")).execute(tx)
    constructDeleteCleanedRelationships().execute(tx)
    constructDeleteDetachedNodes(ElementClass.IO).execute(tx)
    constructDeleteDetachedNodes(ElementClass.Storage).execute(tx)
  }

  // TODO: extract all DDL keywords to GraphElements
  private def constructRelationshipDeleteMarking(
      line:Line,
      startElement: GraphElement
  ): DeferredQuery[Unit] = {
    val query = (
      c"MATCH p =" +  startElement.toSearchObject() + "-[*0..]-> (x)" +
      c"FOREACH ( x IN relationships(p) | SET (CASE WHEN " + c"${line.name}" +
      c"IN x.fromLines THEN x END).toDelete = ${"true"})" +
      c"WITH p" +
      c"MATCH (a) -[r {toDelete: ${"true"}}]-> (b)" +
      c"SET r.fromLines = FILTER ( y IN r.fromLines WHERE y <> ${line.name}), r.toDelete = NULL").query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  private def constructDeleteCleanedRelationships(): DeferredQuery[Unit] = {
    val query =
      c"MATCH (i) <-[s {fromLines: []}]- () DELETE s".query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
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
    val r = R.HasDetails(Map(), "r")
    val query = c""
      .+(line.details.map(ld => GraphElements.constructCreateOrUpdateQuery(
        lineNode,
        Some(r),
        Some(N.Details(ld, "ld")),
        true,
        None,
        Some(lineTaggingSnippet(line, r)))).getOrElse(
        GraphElements.constructDeleteQuery(
          lineNode,
          R.HasDetails(Map(), "ldr"),
          N.Details(Map(), "ld")),
      )).query[Unit]
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
        Some(lineTaggingSnippet(line, r))
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
        Some(lineTaggingSnippet(line, r)))).query[Unit]
    query
  }

  private def constructOutputQuery(line: Line, input: IOElement, output: IOElement): DeferredQuery[Unit] = {
    val inputNode = N.IO(input, "i")
    val r = R.HasOutput(Map(), "r")
    val query = c""
      .+(GraphElements.constructCreateOrUpdateQuery(
        inputNode,
        Some(r),
        Some(N.IO(output, "o")),
        false,
        None,
        Some(lineTaggingSnippet(line, r)))).query[Unit]
    logQueryCreation(query)
    query
  }

  private def lineTaggingSnippet(line: Line, relationship: GraphElement): DeferredQueryBuilder = {
    c"ON MATCH SET (CASE WHEN NOT ${line.name} IN" + relationship.toVariableWithNewField("fromLines") +
      c"THEN" + relationship.toVariable() + c"END).fromLines =" + relationship.toVariableWithNewField("fromLines") + c" + [${line.name}]" +
      c"ON CREATE SET" + relationship.toVariableWithNewField("fromLines") + c"= [${line.name}]"
  }

  private def logQueryCreation(query: DeferredQuery[_]): Unit = {
    logger.debug(s"Creating query:\n${query.query}\nwith params:\n${query.params.map(p => s"${p._1}: ${p._2.toString}")}")
  }
}
