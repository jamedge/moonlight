package edge.jam.moonlight.core.model

import neotypes.{DeferredQuery, Driver}

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
      }
    }
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
        Some(N.Details(ld, "ld")))).getOrElse(
        GraphElements.constructDeleteQuery(
          lineNode,
          R.HasDetails(Map(), "ldr"),
          N.Details(Map(), "ld")))).query[Unit]
    logQueryCreation(query)
    query
  }

  private def logQueryCreation(query: DeferredQuery[Unit]): Unit = {
    logger.debug(s"Creating query: ${query.query} with params ${query.params.map(p => s"${p._1}: ${p._2.toString}")}")
  }
}
