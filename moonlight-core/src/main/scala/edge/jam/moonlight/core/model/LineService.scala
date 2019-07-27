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
          .+(constructMergeOrUpdateQuery(QueryWrapper(lineNode)))
          .query[Unit].execute(tx)
        c""
          .+(line.details.map(ld => constructMergeOrUpdateQuery(
            QueryWrapper(lineNode),
            Some(QueryWrapper(R.HasDetails())),
            Some(QueryWrapper(N.Details(ld, s"ld"))))).getOrElse(c"MATCH (l:Line) -[lds:HAS_DETAILS]-> (ld:Details) DELETE lds, ld"))
          .query[Unit].execute(tx)
      }
    }
  }

  def logQueryCreation(query: DeferredQueryBuilder): DeferredQueryBuilder = {
    val toExecute = query.query[Unit]
    logger.debug(s"Creating query: ${toExecute.query} with params ${toExecute.params.map(p => s"${p._1}: ${p._2.toString}")}")
    query
  }

  def constructMergeOrUpdateQuery(
      node1: QueryWrapper,
      relationship: Option[QueryWrapper] = None,
      node2: Option[QueryWrapper] = None): DeferredQueryBuilder = {
    val query = relationship.flatMap { r =>
      node2.map { n2 =>
        c"MATCH" + node1.so +
          c"MERGE" + node1.ve + r.o + n2.so +
          c"ON MATCH SET" + n2.v + c"=" + n2.f +
          c"ON CREATE SET" + n2.v + c"=" + n2.f
      }
    }.getOrElse(
      c"MERGE" + node1.so +
        c"ON MATCH SET" + node1.v + c"=" + node1.f +
        c"ON CREATE SET" + node1.v + c"=" + node1.f
    )
    logQueryCreation(query)
  }
}

case class QueryWrapper(
    graphElement: GraphElement,
    alreadyExistsInQuery: Boolean = false) {
  def c: DeferredQueryBuilder = {
    if (alreadyExistsInQuery) ve else o
  }

  def ve: DeferredQueryBuilder = {
    graphElement.toVariableEnclosed()
  }

  def v: DeferredQueryBuilder = {
    graphElement.toVariable()
  }

  def o: DeferredQueryBuilder = {
    graphElement.toObject()
  }

  def so: DeferredQueryBuilder = {
    graphElement.toSearchObject()
  }

  def f: DeferredQueryBuilder = {
    graphElement.fields()
  }
}
