package com.github.jamedge.moonlight.core.model.neo4j.queries

import com.github.jamedge.moonlight.core.model.neo4j.NodeClass
import com.github.jamedge.moonlight.core.model.neo4j.{Node, RelationshipRight}
import neotypes.DeferredQueryBuilder
import neotypes.implicits.all._

trait DeleteQueriesConstructor {
  def deleteConnectingNodeAndRelationship(
      matchNode: Node,
      relationshipToDelete: RelationshipRight,
      nodeToDelete: Node): DeferredQueryBuilder = {
    c"MATCH" + matchNode.toObject("mn") + relationshipToDelete.toSearchObject() + nodeToDelete.toSearchObject("nd") +
      c"DELETE" + relationshipToDelete.toVariable() + "," + nodeToDelete.toVariable("nd")
  }

  def deleteDetachedNodesQuery(nodeClass: NodeClass): DeferredQueryBuilder = {
    c"MATCH" + s"(i:${nodeClass.name})" + c"WHERE NOT (i) <-- () DELETE i"
  }

  def deleteCleanedRelationships(): DeferredQueryBuilder = {
    c"MATCH (i) <-[s {fromLines: []}]- () DELETE s"
  }

  def snippetRelationshipTagging(
      tagContainerFieldName: String,
      tagValue: String,
      relationship: RelationshipRight): DeferredQueryBuilder = {
    c"ON MATCH SET (CASE WHEN NOT $tagValue IN" + relationship.toVariableWithNewField(tagContainerFieldName) +
      c"THEN" + relationship.toVariable() + c"END).fromLines =" + relationship.toVariableWithNewField(tagContainerFieldName) + c" + [$tagValue]" +
      c"ON CREATE SET" + relationship.toVariableWithNewField(tagContainerFieldName) + c"= [$tagValue]"
  }

  def prepareRelationshipsForDeletion(
      tagContainerFieldName: String,
      tagValue: String,
      startNode: Node): DeferredQueryBuilder = {
    c"MATCH p =" + startNode.toSearchObject() + "-[*0..]-> (x)" +
      s"FOREACH ( x IN relationships(p) | SET x.$tagContainerFieldName = FILTER ( y IN x.$tagContainerFieldName" +
      c"WHERE y <> $tagValue OR x.relationshipType = ${"permanent"}))"
  }
}
