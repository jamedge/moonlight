package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.GraphElements.{ElementClass, ElementType, GraphElement}
import neotypes.DeferredQueryBuilder
import neotypes.implicits._

object GraphElements {
  abstract class GraphElement(
      elementClass: ElementClass,
      variable: String,
      fieldsPairs: Map[String, _]
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
      }.getOrElse(toAnyObjectOfType())
    }

    /**
     * Gets object of a graph element with just its type. Used to find all objects of that type.
     * e.g. (l:Line)
     */
    def toAnyObjectOfType(): DeferredQueryBuilder = {
      constructObject(c"")
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
     * Gets variable with the specified field.
     * e.g. l.name
     */
    def toVariableWithField(fieldName: String): DeferredQueryBuilder = {
      if (fieldsPairs.contains(fieldName)) {
        c"" + s"$variable.$fieldName"
      } else {
        throw new IllegalAccessException(s"Field name $fieldName of the ${elementClass.name} doesn't exist!")
      }
    }

    /**
     * Gets variable with the specified field even if the field is not defined.
     * e.g. l.name
     */
    def toVariableWithNewField(fieldName: String): DeferredQueryBuilder = {
      c"" + s"$variable.$fieldName"
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

    private def constructFields(pairs: Map[String, _]): DeferredQueryBuilder = {
      val allFields = foldMap(pairs)
      if (pairs.nonEmpty) {
        allFields
      } else
        c""
    }
  }

  def foldList(list: List[DeferredQueryBuilder]): DeferredQueryBuilder = {
    def fold(elements: List[DeferredQueryBuilder], acc: DeferredQueryBuilder): DeferredQueryBuilder = {
      if (elements.isEmpty) {
        c"[" + acc + c"]"
      } else {
        val separator = if (elements.tail.isEmpty) "" else ", "
        fold(elements.tail, acc + elements.head + separator)
      }
    }
    fold(list, c"")
  }

  def foldMap(map: Map[String, _]): DeferredQueryBuilder = {
    def fold(pairs: Map[String, _], acc: DeferredQueryBuilder): DeferredQueryBuilder = {
      if (pairs.isEmpty) {
        c"{" + acc + c"}"
      } else {
        val name = pairs.head._1
        val value = pairs.head._2 match {
          case element: String => c"$element"
          case list: List[_] => foldList(list.map(e => c"${e.toString}"))
          case _ => c""
        }
        val separator = if (pairs.tail.isEmpty) "" else ", "
        fold(pairs.tail, acc + s"$name:" + value + separator)
      }
    }
    fold(map, c"")
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
    case class HasDetails(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_DETAILS", elementType)
    case class HasInput(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_INPUT", elementType)
    case class HasOutput(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_OUTPUT", elementType)
    case class HasStorage(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_STORAGE", elementType)
    case class IsProcessedBy(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("IS_PROCESSED_BY", elementType)
    case class HasProcessingFramework(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_PROCESSING_FRAMEWORK", elementType)
    case class HasMetrics(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_METRIC", elementType)
    case class HasMetricsFramework(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_METRICS_FRAMEWORK", elementType)
    case class HasAlert(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_ALERT", elementType)
    case class HasAlertsFramework(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_ALERTS_FRAMEWORK", elementType)
    case class HasCode(override val elementType: ElementType = ElementType.RelationshipRight) extends ElementClass("HAS_CODE", elementType)
  }
}

object Nodes {
  import edge.jam.moonlight.core.model.{
    Line => ModelLine,
    IOElement => ModelIOElement}

  case class Line(fieldPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Line, GraphElements.generateVariable(variablePrefix), fieldPairs)
  object Line {
    def apply(from: ModelLine, variablePrefix: String): Line = {
      val fields = from.fieldsMap()
      Line(fields, variablePrefix)
    }
  }

  case class IO(fieldPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.IO, GraphElements.generateVariable(variablePrefix), fieldPairs)
  object IO {
    def apply(from: ModelIOElement, variablePrefix: String): IO = {
      val fields = from.fieldsMap() ++ Map(
        "locationRelativePath" -> from.locationRelativePath.getOrElse("")
      )
      IO(fields, variablePrefix)
    }
    def apply(anyObjectVariable: String): IO = {
      IO(IOElement("", None, None, None, None, None, None), anyObjectVariable)
    }
  }

  case class Details(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Storage(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Process(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class ProcessingFramework(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Metric(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class MetricsFramework(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Alert(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class AlertsFramework(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class Code(fieldsPairs: Map[String, _], variablePrefix: String = "")
    extends GraphElement(ElementClass.Details, GraphElements.generateVariable(variablePrefix), fieldsPairs)
}

object Relationships {
  case class HasDetails(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasDetails(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasInput(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasInput(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasOutput(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasOutput(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasStorage(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasStorage(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class IsProcessedBy(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.IsProcessedBy(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasProcessingFramework(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasProcessingFramework(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetrics(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasMetrics(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasMetricsFramework(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasMetricsFramework(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlert(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasAlert(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasAlertsFramework(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasAlertsFramework(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)

  case class HasCode(fieldsPairs: Map[String, _] = Map(), variablePrefix: String = "", elementType: ElementType = ElementType.RelationshipRight)
    extends GraphElement(ElementClass.HasCode(elementType), GraphElements.generateVariable(variablePrefix), fieldsPairs)
}
