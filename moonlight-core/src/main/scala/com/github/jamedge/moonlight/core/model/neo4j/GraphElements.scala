package com.github.jamedge.moonlight.core.model.neo4j

import neotypes.DeferredQueryBuilder
import neotypes.implicits.all._

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
     * e.g. (l:Line {name: "test"}) or (l:Line) if name field doesn't exist
     */
    def toSearchObject(): DeferredQueryBuilder = {
      fieldsPairs.get("name").map { name =>
        constructObject(constructFields(Map("name" -> name)))
      }.getOrElse(toAnyObjectOfType())
    }

    /**
     * Gets search object of a graph element. Used to find it by given field name and value.
     * It contains variable, type and value of the provided parameter.
     * e.g. (l:Line {name: "test"}) or (l:Line) if name is not defined
     */
    def toSearchObjectSpecified(name: String, value: String): DeferredQueryBuilder = {
      constructAnyClassObject(constructFields(Map(name -> value)))
    }

    /**
     * Gets search object of a graph element. Used to find it by given fields map of names and values.
     * It contains variable, type and values of the provided parameter.
     * e.g. (r:HasOutput {isDelete: "true", fromLines: []}) or (r:HasOutput) if fields map is not defined
     */
    def toSearchObjectSpecified(fieldsMap: Map[String, _]): DeferredQueryBuilder = {
      constructAnyClassObject(constructFields(fieldsMap))
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
     * Gets variable enclosed in and object type of a graph element.
     * e.g. (l*1..4)
     */
    def toVariableEnclosedWithCardinality(cardinalityFrom: Option[Int], cardinalityTo: Option[Int]): DeferredQueryBuilder = {
      val from = cardinalityFrom.map(f => s"$f..").getOrElse("..")
      val fromAndTo = cardinalityTo.map(t => s"$from$t").getOrElse(from)
      c"" + s"${elementClass.elementType.openMark}$variable*$fromAndTo${elementClass.elementType.endMark}"
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

    private def constructAnyClassObject(pairs: DeferredQueryBuilder): DeferredQueryBuilder = {
      c"" +
        s"${elementClass.elementType.openMark}$variable" +
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

  abstract class Node(nodeClass: NodeClass, variable: String, fieldsPairs: Map[String, _])
    extends GraphElement(nodeClass, variable, fieldsPairs)

  abstract class RelationshipRight(relationshipRightClass: RelationshipRightClass, variable: String, fieldsPairs: Map[String, _])
    extends GraphElement(relationshipRightClass, variable, fieldsPairs)

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
  sealed class NodeClass(override val name: String) extends ElementClass(name, ElementType.Node)
  sealed class RelationshipRightClass(override val name: String) extends ElementClass(name, ElementType.RelationshipRight)

  object ElementClass {
    // Nodes
    case object Line extends NodeClass("Line")
    case object IO extends NodeClass("IO")
    case object Details extends NodeClass("Details")
    case object Storage extends NodeClass("Storage")
    case object Process extends NodeClass("Process")
    case object ProcessingFramework extends NodeClass("ProcessingFramework")
    case object ProcessingHistory extends NodeClass("ProcessingHistory")
    case object Metric extends NodeClass("Metric")
    case object MetricsFramework extends NodeClass("MetricsFramework")
    case object Alert extends NodeClass("Alert")
    case object AlertsFramework extends NodeClass("AlertFramework")
    case object Code extends NodeClass("Code")

    // Relationships
    case object HasDetails extends RelationshipRightClass("HAS_DETAILS")
    case object HasInput extends RelationshipRightClass("HAS_INPUT")
    case object HasOutput extends RelationshipRightClass("HAS_OUTPUT")
    case object HasStorage extends RelationshipRightClass("HAS_STORAGE")
    case object IsProcessedBy extends RelationshipRightClass("IS_PROCESSED_BY")
    case object HasProcessingFramework extends RelationshipRightClass("HAS_PROCESSING_FRAMEWORK")
    case object HasProcessingHistory extends RelationshipRightClass("HAS_PROCESSING_HISTORY")
    case object HasProcessingHistoryRecord extends RelationshipRightClass("HAS_PROCESSING_HISTORY_RECORD")
    case object HasMetrics extends RelationshipRightClass("HAS_METRIC")
    case object HasMetricsFramework extends RelationshipRightClass("HAS_METRICS_FRAMEWORK")
    case object HasAlert extends RelationshipRightClass("HAS_ALERT")
    case object HasAlertsFramework extends RelationshipRightClass("HAS_ALERTS_FRAMEWORK")
    case object HasCode extends RelationshipRightClass("HAS_CODE")
  }
}
