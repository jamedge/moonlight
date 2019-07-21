package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.GraphElement
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
      session.transact[Unit] { tx =>
        val lineNode = N.Line(line, "l")
        c""
          .+(constructMergeQuery(QueryWrapper(lineNode, false)))
          .+(line.details.map(ld => constructMergeQuery(
            QueryWrapper(lineNode, true),
            Some(QueryWrapper(R.HasDetails(), false)),
            Some(QueryWrapper(N.Details(ld), false)))).getOrElse(""))
          .query[Unit].execute(tx)
        if (line.details.isEmpty) {
          tx.execute[Unit]("MATCH (l:Line) -[lds:HAS_DETAILS]-> (ld:Details) DELETE lds, ld", Map())
        } else tx.commit() // commit should be in the end of the transact block (and now is the end, so it's here) or just return Future()
      }
    }
  }

  def logQueryCreation(query: String): String = {
    logger.debug(s"Creating query: $query")
    query
  }

  def constructMergeQuery(
      node1: QueryWrapper,
      relationship: Option[QueryWrapper] = None,
      node2: Option[QueryWrapper] = None): String = {
    val toMerge = relationship.map { r =>
      node2.map { n2 =>
        s"$node1 $r $n2"
      }.getOrElse(s"$node1 $r")
    }.getOrElse(node1.toString)
    val query = s"MERGE $toMerge"
    logQueryCreation(query)
  }

}

case class QueryWrapper(graphElement: GraphElement, alreadyExistsInQuery: Boolean) {
  override def toString: String = {
    if (alreadyExistsInQuery) {
      graphElement.toVariable()
    } else {
      graphElement.toString()
    }
  }
}
