package com.github.jamedge.moonlight.core.model.neo4j.queries

import com.github.jamedge.moonlight.core.model.neo4j.{GraphElement, Node, Nodes, RelationshipRight, Relationships}
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
    matchConnectingChain(sourceNode, List(ChainLink(DefaultRelationshipRightPresent(Relationships.HasDetails()), DefaultNodePresent(Nodes.Details(), unstructured = true))), lineName)
  }

  def matchConnectingNode(
      sourceNodeName: String,
      relationship: RelationshipRight,
      destinationNode: Node,
      lineName: String
  ): DeferredQueryBuilder = {
    matchConnectingChain(nodeFactory(sourceNodeName), List(ChainLink(DefaultRelationshipRightPresent(relationship), DefaultNodePresent(destinationNode))), lineName)
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
      chain: List[ChainLink], // TODO: without control on each graph element having unique variable it's up to calling method to make them distinct. Think of another way to overcome this without a big overhaul of this method
      lineName: String
  ): DeferredQueryBuilder = {
    if (chain.nonEmpty) {
      c"MATCH" + sourceNode.toSearchObject("sn") +
        chain.foldLeft(c"") {
          case (a: DeferredQueryBuilder, b: ChainLink) =>
            a + b.relationship.value.toAnyObjectOfType() + b.destinationNode.value.toAnyObjectOfType()} +
        chain.tail.foldLeft(snippetRelationshipFromLinesCondition(lineName, trim(chain.head.relationship.value.toVariable()))) {
          case (a: DeferredQueryBuilder, b: ChainLink) =>
            a + c"AND" + snippetRelationshipFromLines(lineName, trim(b.relationship.value.toVariable()))
        } +
      c"RETURN" + chain.tail.foldLeft { // First one needs to show the node, while relationship can be left out
        if (chain.tail.nonEmpty) {
          snippetRelationshipAndNodeVariablesShow(chain.head.relationship, chain.head.destinationNode)
        } else {
          chain.head.relationship.valueVariableShouldShow +
            (if (trim(chain.head.relationship.valueVariableShouldShow) != "") c"," else c"") +
            chain.head.destinationNode.valueVariable
        }
      } {
        case (a: DeferredQueryBuilder, b: ChainLink) =>
          a +
            (if (a.query[String].query.trim != "") c"," else c"") +
            snippetRelationshipAndNodeVariablesShow(b.relationship, b.destinationNode)
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

  private def snippetRelationshipAndNodeVariablesShow(relationship: Present[RelationshipRight], node: Present[Node]): DeferredQueryBuilder = {
    relationship.valueVariableShouldShow +
      (if (trim(relationship.valueVariableShouldShow) != "") c"," else c"") +
      node.valueVariableShouldShow
  }
}

// TODO: try to add a way to present just name of the node as string
case class ChainLink(relationship: Present[RelationshipRight], destinationNode: Present[Node])

object ChainLink {
  def apply(relationship: RelationshipRight, destinationNode: Node): ChainLink = {
    ChainLink(DefaultRelationshipRightPresent(relationship), DefaultNodePresent(destinationNode))
  }

  def apply(relationship: RelationshipRight, destinationNode: Present[Node]): ChainLink = {
    ChainLink(DefaultRelationshipRightPresent(relationship), destinationNode)
  }

  def apply(relationship: Present[RelationshipRight], destinationNode: Node): ChainLink = {
    ChainLink(relationship, DefaultNodePresent(destinationNode))
  }
}

class Present[T <: GraphElement](val value: T, val show: Boolean, val unstructured: Boolean) {
  val valueVariable: DeferredQueryBuilder = if (unstructured) {
    c"" + s"${value.toVariable().query[String].query.trim} {.*}"
  } else {
    value.toVariable()
  }

  val valueVariableShouldShow: DeferredQueryBuilder = if (show) {
    valueVariable
  } else {
    c""
  }
}

case class DefaultNodePresent(node: Node, override val show: Boolean = true, override val unstructured: Boolean = false)
  extends Present[Node](node, show, unstructured)
case class DefaultRelationshipRightPresent(relationshipRight: RelationshipRight, override val show: Boolean = false, override val unstructured: Boolean = false)
  extends Present[RelationshipRight](relationshipRight, show, unstructured)

case class MatchingException(message: String) extends Exception(message)
