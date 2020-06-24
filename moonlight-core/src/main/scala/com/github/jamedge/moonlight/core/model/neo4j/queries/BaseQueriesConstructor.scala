package com.github.jamedge.moonlight.core.model.neo4j.queries

import com.github.jamedge.moonlight.core.model.neo4j.{Node, Nodes, RelationshipRight, Relationships}
import neotypes.DeferredQueryBuilder
import neotypes.implicits.all._

class BaseQueriesConstructor[T <: Node](nodeFactory: String => T) {
  def matchNode(name: String): DeferredQueryBuilder = {
    matchNode(nodeFactory(name))
  }

  def matchNode(sourceNode: T): DeferredQueryBuilder = {
    c"MATCH" + sourceNode.toSearchObject() + c"RETURN" + sourceNode.toVariable()
  }

  def matchDetails(sourceNodeName: String, lineName: String): DeferredQueryBuilder = {
    matchDetails(nodeFactory(sourceNodeName), lineName)
  }

  def matchDetails(sourceNode: T, lineName: String): DeferredQueryBuilder = {
    matchConnectingNodes(sourceNode, List(Chain(Relationships.HasDetails(), Nodes.Details(), unstructured = true)), lineName)
  }

  def matchConnectingNodes(
      sourceNodeName: String,
      relationship: RelationshipRight,
      destinationNode: Node,
      lineName: String
  ): DeferredQueryBuilder = {
    matchConnectingNodes(nodeFactory(sourceNodeName), List(Chain(relationship, destinationNode)), lineName)
  }

  def matchConnectingNodes(
      sourceNodeName: String,
      chainList: List[Chain],
      lineName: String
  ): DeferredQueryBuilder = {
    matchConnectingNodes(nodeFactory(sourceNodeName), chainList, lineName)
  }

  def matchConnectingNodes(
      sourceNode: T,
      chainList: List[Chain],
      lineName: String
  ): DeferredQueryBuilder = {
    if (chainList.nonEmpty) {
      c"MATCH" + sourceNode.toSearchObject() +
        chainList.foldLeft(c"") {
          case (a: DeferredQueryBuilder, b: Chain) =>
            a + b.relationship.toAnyObjectOfType() + b.destinationNode.toAnyObjectOfType()} +
        chainList.tail.foldLeft(snippetRelationshipFromLinesCondition(lineName, trim(chainList.head.relationship.toVariable()))) {
          case (a: DeferredQueryBuilder, b: Chain) =>
            a + c"AND" + snippetRelationshipFromLines(lineName, trim(b.relationship.toVariable()))
        } +
      c"RETURN" + chainList.tail.foldLeft(chainList.head.destinationNodeVariable) {
        case (a: DeferredQueryBuilder, b: Chain) =>
          a + c"," + b.destinationNodeVariable
      }
    } else {
      throw MatchingException("Input chain list must not be empty!")
    }
  }

  protected def snippetRelationshipFromLines(lineName: String, relationshipVariable: String): DeferredQueryBuilder = {
    c"$lineName IN" + s"$relationshipVariable.fromLines"
  }

  protected def snippetRelationshipFromLinesCondition(lineName: String, relationshipVariable: String): DeferredQueryBuilder = {
    c"WHERE" + snippetRelationshipFromLines(lineName, relationshipVariable)
  }

  private def trim(snippet: DeferredQueryBuilder): String = {
    snippet.query[String].query.trim
  }
}

case class Chain(relationship: RelationshipRight, destinationNode: Node, unstructured: Boolean = false) {
  val destinationNodeVariable: DeferredQueryBuilder = if (unstructured) {
    c"" + s"${destinationNode.toVariable().query[String].query.trim} {.*}"
  } else {
    destinationNode.toVariable()
  }
}
case class MatchingException(message: String) extends Exception(message)
