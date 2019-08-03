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
          .+(constructMergeOrUpdateQuery(lineNode))
          .query[Unit].execute(tx)
        c""
          .+(line.details.map(ld => constructMergeOrUpdateQuery(
            lineNode,
            Some(R.HasDetails()),
            Some(N.Details(ld, "ld")))).getOrElse(
              constructDeleteNodeAndRelatedRelationshipQuery(
                lineNode,
                R.HasDetails(Map(), "ldr"),
                N.Details(Map(), "ld"))))
          .query[Unit].execute(tx)
      }
    }
  }

  def logQueryCreation(query: DeferredQueryBuilder): Unit = {
    val toExecute = query.query[Unit]
    logger.debug(s"Creating query: ${toExecute.query} with params ${toExecute.params.map(p => s"${p._1}: ${p._2.toString}")}")
  }

  def constructMergeOrUpdateQuery(
      node1: GraphElement,
      relationship: Option[GraphElement] = None,
      node2: Option[GraphElement] = None): DeferredQueryBuilder = {
    val query = relationship.flatMap { r =>
      node2.map { n2 =>
        c"MATCH" + node1.toSearchObject() +
          c"MERGE" + node1.toVariableEnclosed() + r.toObject() + n2.toSearchObject() +
          c"ON MATCH SET" + n2.toVariable() + c"=" + n2.fields() +
          c"ON CREATE SET" + n2.toVariable() + c"=" + n2.fields()
      }
    }.getOrElse(
      c"MERGE" + node1.toSearchObject() +
        c"ON MATCH SET" + node1.toVariable() + c"=" + node1.fields() +
        c"ON CREATE SET" + node1.toVariable() + c"=" + node1.fields()
    )
    logQueryCreation(query)
    query
  }

  def constructDeleteNodeAndRelatedRelationshipQuery(
      matchNode: GraphElement,
      relationshipToDelete: GraphElement,
      nodeToDelete: GraphElement): DeferredQueryBuilder = {
    val query = c"MATCH" + matchNode.toObject() + relationshipToDelete.toSearchObject() + nodeToDelete.toSearchObject() +
      c"DELETE" + relationshipToDelete.toVariable() + "," + nodeToDelete.toVariable()
    logQueryCreation(query)
    query
  }

}
