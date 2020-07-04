package com.github.jamedge.moonlight.core.model.neo4j

sealed class ElementType(
    val openMark: String,
    val endMark: String)

object ElementType {
  case object Node extends ElementType("(", ")")
  case object RelationshipUndirected extends ElementType("-[", "]-")
  case object RelationshipLeft extends ElementType("<-[", "]-")
  case object RelationshipRight extends ElementType("-[", "]->")
  case object RelationshipBoth extends ElementType("<-[", "]->")
}
