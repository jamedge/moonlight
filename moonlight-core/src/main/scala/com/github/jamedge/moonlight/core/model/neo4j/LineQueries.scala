package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}
import neotypes.DeferredQueryBuilder
import neotypes.implicits.all._

object LineQueries {
  def constructCreateOrUpdateQuery(
      node1: GraphElement,
      relationship: Option[GraphElement] = None,
      node2: Option[GraphElement] = None,
      createDuplicateNode2IfPathNotFound: Boolean = false,
      matchArgument: Option[DeferredQueryBuilder] = None,
      relationshipSetArgument: Option[DeferredQueryBuilder] = None
  ): DeferredQueryBuilder = {
    relationship.flatMap { r =>
      node2.map { n2 =>
        val result = if (createDuplicateNode2IfPathNotFound) {
          c"MATCH" + matchArgument.getOrElse(node1.toSearchObject()) +
            c"MERGE" + node1.toVariableEnclosed() + r.toObject() + n2.toSearchObject() +
            c"ON MATCH SET" + n2.toVariable() + c"+=" + n2.fields() +
            c"ON CREATE SET" + n2.toVariable() + c"=" + n2.fields()
        } else {
          c"MATCH" + matchArgument.getOrElse(node1.toSearchObject()) +
            c"MERGE" + n2.toSearchObject() +
            c"ON MATCH SET" + n2.toVariable() + c"+=" + n2.fields() +
            c"ON CREATE SET" + n2.toVariable() + c"=" + n2.fields() +
            c"MERGE" + node1.toVariableEnclosed() + r.toObject() + n2.toVariableEnclosed()
        }
        relationshipSetArgument.map { rsa =>
          result + rsa
        }.getOrElse(result)
      }
    }.getOrElse(
      c"MERGE" + node1.toSearchObject() +
        c"ON MATCH SET" + node1.toVariable() + c"+=" + node1.fields() +
        c"ON CREATE SET" + node1.toVariable() + c"=" + node1.fields()
    )
  }

  def constructDeleteQuery(
      matchNode: GraphElement,
      relationshipToDelete: GraphElement,
      nodeToDelete: GraphElement): DeferredQueryBuilder = {
    c"MATCH" + matchNode.toObject() + relationshipToDelete.toSearchObject() + nodeToDelete.toSearchObject() +
      c"DELETE" + relationshipToDelete.toVariable() + "," + nodeToDelete.toVariable()
  }

  def constructDeleteDetachedNodesQuery(elementClass: ElementClass): DeferredQueryBuilder = {
    c"MATCH" + s"(i:${elementClass.name})" + c"WHERE NOT (i) <-- () DELETE i"
  }

  def constructDeleteCleanedRelationshipsQuery(): DeferredQueryBuilder = {
    c"MATCH (i) <-[s {fromLines: []}]- () DELETE s"
  }

  def constructRelationshipTaggingSnippet(
      tagContainerFieldName: String,
      tagValue: String,
      relationship: GraphElement): DeferredQueryBuilder = {
    c"ON MATCH SET (CASE WHEN NOT ${tagValue} IN" + relationship.toVariableWithNewField(tagContainerFieldName) +
      c"THEN" + relationship.toVariable() + c"END).fromLines =" + relationship.toVariableWithNewField(tagContainerFieldName) + c" + [$tagValue]" +
      c"ON CREATE SET" + relationship.toVariableWithNewField(tagContainerFieldName) + c"= [$tagValue]"
  }

  def constructRelationshipDeleteMarkingQuery(
      tagContainerFieldName: String,
      tagValue: String,
      startElement: GraphElement): DeferredQueryBuilder = {
    c"MATCH p =" + startElement.toSearchObject() + "-[*0..]-> (x)" +
      s"FOREACH ( x IN relationships(p) | SET x.$tagContainerFieldName = FILTER ( y IN x.$tagContainerFieldName" +
      c"WHERE y <> $tagValue OR x.relationshipType = ${"permanent"}))"
  }
}
