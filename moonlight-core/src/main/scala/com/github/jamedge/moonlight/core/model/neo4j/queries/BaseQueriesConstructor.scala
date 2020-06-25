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
    matchConnectingChain(sourceNode, List(ChainLink(Relationships.HasDetails(), Nodes.Details(), unstructured = true)), lineName)
  }

  def matchConnectingNode(
      sourceNodeName: String,
      relationship: RelationshipRight,
      destinationNode: Node,
      lineName: String
  ): DeferredQueryBuilder = {
    matchConnectingChain(nodeFactory(sourceNodeName), List(ChainLink(relationship, destinationNode)), lineName)
  }

  def matchConnectingChain(
      sourceNodeName: String,
      chain: List[ChainLink],
      lineName: String
  ): DeferredQueryBuilder = {
    matchConnectingChain(nodeFactory(sourceNodeName), chain, lineName)
  }

  // TODO: split this into snippet functions
  def matchConnectingChain(
      sourceNode: T,
      chain: List[ChainLink],
      lineName: String
  ): DeferredQueryBuilder = {
    if (chain.nonEmpty) {
      c"MATCH" + sourceNode.toSearchObject() +
        chain.foldLeft(c"") {
          case (a: DeferredQueryBuilder, b: ChainLink) =>
            a + b.relationship.toAnyObjectOfType() + b.destinationNode.toAnyObjectOfType()} +
        chain.tail.foldLeft(snippetRelationshipFromLinesCondition(lineName, trim(chain.head.relationship.toVariable()))) {
          case (a: DeferredQueryBuilder, b: ChainLink) =>
            a + c"AND" + snippetRelationshipFromLines(lineName, trim(b.relationship.toVariable()))
        } +
      c"RETURN" + chain.tail.foldLeft(if (chain.tail.nonEmpty) chain.head.destinationNodeVariableShouldShow else chain.head.destinationNodeVariable) {
        case (a: DeferredQueryBuilder, b: ChainLink) =>
          a + (if (a.query[String].query.trim != "") c"," else c"") + b.destinationNodeVariableShouldShow
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

// TODO: try to add a way to present just name of the node as string
case class ChainLink(relationship: RelationshipRight, destinationNode: Node, show: Boolean = true, unstructured: Boolean = false) {
  val destinationNodeVariable: DeferredQueryBuilder = if (unstructured) {
      c"" + s"${destinationNode.toVariable().query[String].query.trim} {.*}"
    } else {
      destinationNode.toVariable()
    }

  val destinationNodeVariableShouldShow: DeferredQueryBuilder = if (show) {
    destinationNodeVariable
  } else {
    c""
  }
}
case class MatchingException(message: String) extends Exception(message)
