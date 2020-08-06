package com.github.jamedge.moonlight.core.model.neo4j.queries

import com.github.jamedge.moonlight.core.model.neo4j.{Node, RelationshipRight}
import neotypes.DeferredQueryBuilder
import neotypes.implicits.all._

trait CreateQueriesConstructor {
  def createOrUpdate(
      node1: Node,
      relationship: Option[RelationshipRight] = None,
      node2: Option[Node] = None,
      createDuplicateNode2IfPathNotFound: Boolean = false,
      matchArgument: Option[DeferredQueryBuilder] = None,
      relationshipSetArgument: Option[DeferredQueryBuilder] = None
  ): DeferredQueryBuilder = {
    relationship.flatMap { r =>
      node2.map { n2 =>
        val result = if (createDuplicateNode2IfPathNotFound) {
          c"MATCH" + matchArgument.getOrElse(node1.toSearchObject("1")) +
            c"MERGE" + node1.toVariableEnclosed("1") + r.toObject() + n2.toSearchObject("2") +
            c"ON MATCH SET" + n2.toVariable("2") + c"+=" + n2.fields() +
            c"ON CREATE SET" + n2.toVariable("2") + c"=" + n2.fields()
        } else {
          c"MATCH" + matchArgument.getOrElse(node1.toSearchObject("1")) +
            c"MERGE" + n2.toSearchObject("2") +
            c"ON MATCH SET" + n2.toVariable("2") + c"+=" + n2.fields() +
            c"ON CREATE SET" + n2.toVariable("2") + c"=" + n2.fields() +
            c"MERGE" + node1.toVariableEnclosed("1") + r.toObject() + n2.toVariableEnclosed("2")
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
}
