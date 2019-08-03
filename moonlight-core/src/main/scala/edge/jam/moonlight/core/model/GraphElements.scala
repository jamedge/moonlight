package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.{ElementClass, GraphElement}
import neotypes.DeferredQueryBuilder
import neotypes.implicits._

object GraphElements {
  abstract class GraphElement(
      elementClass: ElementClass,
      variable: String,
      fieldsPairs: Map[String, String]
  ) {

    /**
     * Gets full object definition of a graph element.
     * It contains variable, type and all defined fields.
     * e.g. (l:Line {name: "test", owner: "John Doe"})
     */
    def toObject(): DeferredQueryBuilder = {
      constructObject(fields())
    }

    /**
     * Gets search object of a graph element. Used to find it by name.
     * It contains variable, type and name parameter if defined.
     * e.g. (l:Line {name: "test"}) or (l:Line) if name is not defined
     */
    def toSearchObject(): DeferredQueryBuilder = {
      fieldsPairs.get("name").map { name =>
        constructObject(constructFields(Map("name" -> name)))
      }.getOrElse(constructObject(c""))
    }

    /**
     * Gets variable of a graph element.
     * e.g. l
     */
    def toVariable(): DeferredQueryBuilder = {
      c"" + variable
    }

    /**
     * Gets variable enclosed in and object type of a graph element.
     * e.g. (l)
     */
    def toVariableEnclosed(): DeferredQueryBuilder = {
      c"" + s"${elementClass.elementType.openMark}$variable${elementClass.elementType.endMark}"
    }

    /**
     * Gets fields a graph element.
     * e.g. {name: "test", owner: "John Doe"}
     */
    def fields(): DeferredQueryBuilder = {
      constructFields(fieldsPairs)
    }

    private def constructObject(pairs: DeferredQueryBuilder): DeferredQueryBuilder = {
      c"" +
        s"${elementClass.elementType.openMark}$variable:${elementClass.name}" +
        pairs +
        s"${elementClass.elementType.endMark}"
    }

    private def constructFields(pairs: Map[String, String]): DeferredQueryBuilder = {
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

  def generateVariable(prefix: String): String = {
    if (prefix != "") {
      s"${prefix}_${java.util.UUID.randomUUID.toString.replace("-", "")}"
    } else {
      ""
    }
  }

  def constructCreateOrUpdateQuery(
      node1: GraphElement,
      relationship: Option[GraphElement] = None,
      node2: Option[GraphElement] = None,
      createDuplicates: Boolean = false,
      matchArgument: Option[DeferredQueryBuilder] = None
  ): DeferredQueryBuilder = {
    relationship.flatMap { r =>
      node2.map { n2 =>
        val specific = if (createDuplicates) n2.toObject() else n2.toSearchObject()
        c"MATCH" + matchArgument.getOrElse(node1.toSearchObject()) +
          c"MERGE" + specific +
          c"ON MATCH SET" + n2.toVariable() + c"=" + n2.fields() +
          c"ON CREATE SET" + n2.toVariable() + c"=" + n2.fields() +
          c"MERGE" + node1.toVariableEnclosed() + r.toObject() + n2.toVariableEnclosed()
      }
    }.getOrElse(
      c"MERGE" + node1.toSearchObject() +
        c"ON MATCH SET" + node1.toVariable() + c"=" + node1.fields() +
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
    case object IO extends ElementClass("IO", ElementType.Node)
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
  import edge.jam.moonlight.core.model.{Line => ModelLine, IOElement => ModelIOElement}

  case class Line(fieldPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Line, GraphElements.generateVariable(variablePrefix), fieldPairs)
  object Line {
    def apply(from: ModelLine, variablePrefix: String): Line = {
      val fields = from.fieldsMap()
      Line(fields, variablePrefix)
    }
  }

  case class IO(fieldPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.IO, GraphElements.generateVariable(variablePrefix), fieldPairs)
  object IO {
    def apply(from: ModelIOElement, variablePrefix: String): IO = {
      val fields = from.fieldsMap() ++ Map(
        "locationRelativePath" -> from.locationRelativePath.getOrElse("")
      )
      IO(fields, variablePrefix)
    }
  }

  case class Details(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Storage(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Process(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class ProcessingFramework(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Metric(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class MetricsFramework(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Alert(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class AlertsFramework(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Code(fieldsPairs: Map[String, String], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)
}

object Relationships {
  case class HasDetails(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasDetails, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasInput(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasInput, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasOutput(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasOutput, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasStorage(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasStorage, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class IsProcessedBy(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.IsProcessedBy, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingFramework(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasProcessingFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetrics(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasMetrics, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetricsFramework(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasMetricsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlert(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasAlert, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlertsFramework(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasAlertsFramework, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasCode(fieldsPairs: Map[String, String] = Map(), variablePrefix: String = "")
    extends GraphElement(ElementClass.HasCode, GraphElements.generateVariable(variablePrefix), fieldsPairs)
}
