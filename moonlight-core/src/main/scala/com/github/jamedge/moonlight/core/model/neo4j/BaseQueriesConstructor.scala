package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.GraphElements.GraphElement
import neotypes.DeferredQueryBuilder
import neotypes.implicits.all._

class BaseQueriesConstructor[T <: GraphElement](nodeFactory: String => T) {
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
    c"MATCH" + sourceNode.toSearchObject()  + c"-[lhd:HAS_DETAILS]-> (d)" +
      snippetRelationshipFromLinesCondition(lineName, "lhd") +
      c"RETURN d {.*} AS details"
  }

  protected def snippetRelationshipFromLines(lineName: String, relationshipLabel: String): DeferredQueryBuilder = {
    c"$lineName IN" + s"$relationshipLabel.fromLines"
  }

  protected def snippetRelationshipFromLinesCondition(lineName: String, relationshipLabel: String): DeferredQueryBuilder = {
    c"WHERE" + snippetRelationshipFromLines(lineName, relationshipLabel)
  }


}
