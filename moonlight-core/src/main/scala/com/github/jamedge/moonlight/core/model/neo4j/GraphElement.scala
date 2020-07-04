package com.github.jamedge.moonlight.core.model.neo4j

import com.github.jamedge.moonlight.core.model.neo4j.GraphElementUtils.foldMap
import neotypes.DeferredQueryBuilder

import neotypes.implicits.all._

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

abstract class Node(nodeClass: NodeClass, variable: String, fieldsPairs: Map[String, _])
  extends GraphElement(nodeClass, variable, fieldsPairs)

abstract class RelationshipRight(relationshipRightClass: RelationshipRightClass, variable: String, fieldsPairs: Map[String, _])
  extends GraphElement(relationshipRightClass, variable, fieldsPairs)
