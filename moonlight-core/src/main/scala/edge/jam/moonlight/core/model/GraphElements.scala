package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.{ElementClass, GraphElement}

object GraphElements {
  abstract class GraphElement(
      elementClass: ElementClass,
      variable: String,
      fields: Map[String, String]
  ) {
    override def toString(): String = {
      val fieldsString = if (fields.isEmpty) "" else s" {${constructPairs(fields)}}"
      s"${elementClass.elementType.openMark}$variable:${elementClass.name}$fieldsString${elementClass.elementType.endMark}"
    }

    def constructPairs(pairs: Map[String, String]): String = {
      pairs.map { case (name, value) =>
        s"""$name: "$value""""
      }.mkString(", ")
    }
  }

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

  sealed class ElementClass(val name: String, val elementType: ElementType)
  object ElementClass {
    case object Line extends ElementClass("Line", ElementType.Node)
    case object Details extends ElementClass("Details", ElementType.Node)
    case object HasDetails extends ElementClass("HAS_DETAILS", ElementType.RelationshipRight)
  }
}

object Nodes {
  case class Line(fields: Map[String, String], variable: String = "l")
    extends GraphElement(ElementClass.Line, variable, fields)

  case class LineDetails(fields: Map[String, String], variable: String = "ld")
    extends GraphElement(ElementClass.Details, variable, fields)
}

object Relationships {
  case class HasDetails(fields: Map[String, String] = Map())
    extends GraphElement(ElementClass.HasDetails, "", fields)
}
