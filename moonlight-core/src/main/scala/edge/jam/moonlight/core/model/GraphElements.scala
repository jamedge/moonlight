package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.{ElementClass, GraphElement}
import neotypes.DeferredQueryBuilder
import neotypes.implicits._

object GraphElements {
  abstract class GraphElement(
      elementClass: ElementClass,
      variable: String,
      fields: Map[String, String]
  ) {

    def toObject(): DeferredQueryBuilder = {
      val pairs = constructPairs(fields)
      c"" +
        s"${elementClass.elementType.openMark}$variable:${elementClass.name}" +
        pairs +
        s"${elementClass.elementType.endMark}"
    }

    def toVariable(): DeferredQueryBuilder = {
      c"" + s"${elementClass.elementType.openMark}$variable${elementClass.elementType.endMark}"
    }

    def constructPairs(pairs: Map[String, String]): DeferredQueryBuilder = {
      def fold(pairs: Map[String, String], query: DeferredQueryBuilder): DeferredQueryBuilder = {
        if (pairs.isEmpty) {
          c"{" + query + c"}"
        } else {
          val name = pairs.head._1
          val value = pairs.head._2
          val separator = if (pairs.tail.isEmpty) "" else ", "
          fold(pairs.tail, query + s"$name:" + c"$value" + separator)
        }
      }
      val allFields = fold(pairs, c"")
      if (pairs.nonEmpty) {
        allFields
      } else
        c""
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
    // Nodes
    case object Line extends ElementClass("Line", ElementType.Node)
    case object Details extends ElementClass("Details", ElementType.Node)
    case object Storage extends ElementClass("Storage", ElementType.Node)
    case object Process extends ElementClass("Process", ElementType.Node)
    case object ProcessingFramework extends ElementClass("ProcessingFramework", ElementType.Node)
    case object Metric extends ElementClass("Metric", ElementType.Node)
    case object MetricsFramework extends ElementClass("MetricsFramework", ElementType.Node)
    case object Alert extends ElementClass("Alert", ElementType.Node)
    case object AlertsFramework extends ElementClass("AlertFramework", ElementType.Node)
    case object Code extends ElementClass("Code", ElementType.Node)
    // Relationships
    case object HasDetails extends ElementClass("HAS_DETAILS", ElementType.RelationshipRight)
    case object HasInput extends ElementClass("HAS_INPUT", ElementType.RelationshipRight)
    case object HasOutput extends ElementClass("HAS_OUTPUT", ElementType.RelationshipRight)
    case object HasStorage extends ElementClass("HAS_STORAGE", ElementType.RelationshipRight)
    case object IsProcessedBy extends ElementClass("IS_PROCESSED_BY", ElementType.RelationshipRight)
    case object HasProcessingFramework extends ElementClass("HAS_PROCESSING_FRAMEWORK", ElementType.RelationshipRight)
    case object HasMetrics extends ElementClass("HAS_METRIC", ElementType.RelationshipRight)
    case object HasMetricsFramework extends ElementClass("HAS_METRICS_FRAMEWORK", ElementType.RelationshipRight)
    case object HasAlert extends ElementClass("HAS_ALERT", ElementType.RelationshipRight)
    case object HasAlertsFramework extends ElementClass("HAS_ALERTS_FRAMEWORK", ElementType.RelationshipRight)
    case object HasCode extends ElementClass("HAS_CODE", ElementType.RelationshipRight)
  }
}

object Nodes {
  import edge.jam.moonlight.core.model.{Line => ModelLine}

  case class Line(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Line, variable, fields)
  object Line {
    def apply(from: ModelLine, variable: String): Line = {
      val fields = Map(
        "name" -> from.name,
        "owner" -> from.owner.getOrElse(""),
        "purpose" -> from.purpose.getOrElse(""),
        "notes" -> s"[${from.notes.getOrElse(List()).map(el => s""""$el"""").mkString(", ")}]"
      )
      Line(fields, variable)
    }
  }

  case class Details(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class Storage(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class Process(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class ProcessingFramework(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class Metric(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class MetricsFramework(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class Alert(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class AlertsFramework(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)

  case class Code(fields: Map[String, String], variable: String = "")
    extends GraphElement(ElementClass.Details, variable, fields)
}

object Relationships {
  case class HasDetails(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasInput(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasOutput(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasStorage(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class IsProcessedBy(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasProcessingFramework(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasMetrics(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasMetricsFramework(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasAlert(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasAlertsFramework(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)

  case class HasCode(fields: Map[String, String] = Map(), variable: String = "")
    extends GraphElement(ElementClass.HasDetails, variable, fields)
}
