package edge.jam.moonlight.core.model

import neotypes.Driver

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
    neo4jDriver.writeSession { session =>
      val lineNode = N.Line(line, "l")
      c"".+(mergeQuery(lineNode))
        .+(detailsQuery(lineNode, line.details.getOrElse(Map())))
        .query[Unit].execute(session)
    }
  }

  def logQueryCreation(query: String): String = {
    logger.debug(s"Creating query: $query")
    query
  }

  def mergeQuery(node: GraphElements.GraphElement): String = {
    val query = s"MERGE $node"
    logQueryCreation(query)
  }

  def detailsQuery(parentNode: GraphElements.GraphElement, details: Map[String, String]): String = {
    val query = s"MERGE ${parentNode.toVariable()} ${R.HasDetails()} ${N.Details(details)}"
    logQueryCreation(query)
  }

}
