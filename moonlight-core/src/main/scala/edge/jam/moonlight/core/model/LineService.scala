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
        constructCreateOrUpdateQuery(line, N.Line(line, "l")).execute(tx)
        line.details.map(ld =>
          constructCreateOrUpdateQuery(
            line,
            N.Line(line, "l"),
            Some(R.HasDetails(Map(), "r")),
            Some(N.Details(ld, "ld")),
            true).execute(tx)).getOrElse(Future())
        line.io.flatMap { io =>
          io.inputs.flatMap { input =>
            constructCreateOrUpdateQuery(
              line,
              N.Line(line, "l"),
              Some(R.HasInput(Map(), "r")),
              Some(N.IO(input, "i"))).execute(tx)
            input.storage.map(s =>
              constructCreateOrUpdateQuery(
                line,
                N.IO(input, "i"),
                Some(R.HasStorage(Map(), "r")),
                Some(N.Storage(s, "s"))).execute(tx)).getOrElse(Future())
            io.outputs.map { output =>
              constructCreateOrUpdateQuery(
                line,
                N.IO(input, "i"),
                Some(R.HasOutput(Map(), "r")),
                Some(N.IO(output, "o"))).execute(tx)
              output.storage.map(s =>
                constructCreateOrUpdateQuery(
                  line,
                  N.IO(output, "o"),
                  Some(R.HasStorage(Map(), "r")),
                  Some(N.Storage(s, "s"))).execute(tx)).getOrElse(Future())
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
      c"FOREACH ( x IN relationships(p) | SET x.fromLines = FILTER ( y IN x.fromLines WHERE y <> ${line.name}))").query[Unit]
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

  private def constructCreateOrUpdateQuery(
      line: Line,
      n1: GraphElement,
      r: Option[GraphElement] = None,
      n2: Option[GraphElement] = None,
      createDuplicateNode2IfPathNotFound: Boolean = false,
  ): DeferredQuery[Unit] = {
    val query = c""
      .+(GraphElements.constructCreateOrUpdateQuery(
        n1,
        r,
        n2,
        createDuplicateNode2IfPathNotFound,
        None,
        r.map(lineTaggingSnippet(line, _)))).query[Unit]
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
