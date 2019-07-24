package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.GraphElement
import neotypes.{DeferredQueryBuilder, Driver}

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
            Some(QueryWrapper(N.Details(ld), false)))).getOrElse(c""))
          .query[Unit].execute(tx)
        if (line.details.isEmpty) {
          tx.execute[Unit]("MATCH (l:Line) -[lds:HAS_DETAILS]-> (ld:Details) DELETE lds, ld", Map())
        } else Future() //tx.commit() // commit should be in the end of the transact block (and now is the end, so it's here) or just return Future()
      }
    }
  }

  def logQueryCreation(query: DeferredQueryBuilder): DeferredQueryBuilder = {
    val toExecute = query.query[Unit]
    logger.debug(s"Creating query: ${toExecute.query} with params ${toExecute.params.map(p => s"${p._1}: ${p._2.toString}")}")
    query
  }

  def constructMergeQuery( // TODO: fix merging as it creates new nodes if pattern is not found instead of updating the existing pattern by name to fit changes
      node1: QueryWrapper, // TODO: so matching by name needs to be done first and then create if not exist or update if it does
      relationship: Option[QueryWrapper] = None,
      node2: Option[QueryWrapper] = None): DeferredQueryBuilder = {
    val toMerge = relationship.map { r =>
      node2.map { n2 =>
        node1.c + " " + r.c + " " + n2.c
      }.getOrElse(node1.c + " " + r.c)
    }.getOrElse(node1.c)
    val query = c"MERGE " + toMerge
    logQueryCreation(query)
  }

}

case class QueryWrapper(graphElement: GraphElement, alreadyExistsInQuery: Boolean) {
  def c: DeferredQueryBuilder = {
    if (alreadyExistsInQuery) {
      graphElement.toVariable()
    } else {
      graphElement.toObject()
    }
  }
}
