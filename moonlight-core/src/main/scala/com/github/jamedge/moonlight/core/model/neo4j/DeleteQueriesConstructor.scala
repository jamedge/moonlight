package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}
import neotypes.DeferredQueryBuilder

import neotypes.implicits.all._

trait DeleteQueriesConstructor {
  def deleteConnectingNodeAndRelationship(
      matchNode: GraphElement,
      relationshipToDelete: GraphElement,
      nodeToDelete: GraphElement): DeferredQueryBuilder = {
    c"MATCH" + matchNode.toObject() + relationshipToDelete.toSearchObject() + nodeToDelete.toSearchObject() +
      c"DELETE" + relationshipToDelete.toVariable() + "," + nodeToDelete.toVariable()
  }

  def deleteDetachedNodesQuery(elementClass: ElementClass): DeferredQueryBuilder = {
    c"MATCH" + s"(i:${elementClass.name})" + c"WHERE NOT (i) <-- () DELETE i"
  }

  def deleteCleanedRelationships(): DeferredQueryBuilder = {
    c"MATCH (i) <-[s {fromLines: []}]- () DELETE s"
  }

  def snippetRelationshipTagging(
      tagContainerFieldName: String,
      tagValue: String,
      relationship: GraphElement): DeferredQueryBuilder = {
    c"ON MATCH SET (CASE WHEN NOT ${tagValue} IN" + relationship.toVariableWithNewField(tagContainerFieldName) +
      c"THEN" + relationship.toVariable() + c"END).fromLines =" + relationship.toVariableWithNewField(tagContainerFieldName) + c" + [$tagValue]" +
      c"ON CREATE SET" + relationship.toVariableWithNewField(tagContainerFieldName) + c"= [$tagValue]"
  }

  def prepareRelationshipsForDeletion(
      tagContainerFieldName: String,
      tagValue: String,
      startElement: GraphElement): DeferredQueryBuilder = {
    c"MATCH p =" + startElement.toSearchObject() + "-[*0..]-> (x)" +
      s"FOREACH ( x IN relationships(p) | SET x.$tagContainerFieldName = FILTER ( y IN x.$tagContainerFieldName" +
      c"WHERE y <> $tagValue OR x.relationshipType = ${"permanent"}))"
  }
}
