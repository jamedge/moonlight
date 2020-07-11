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
  def toObject(variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    constructObject(fields(), variableSuffixOverride)
  }

  /**
   * Gets search object of a graph element. Used to find it by name.
   * It contains variable, type and name parameter if defined.
   * e.g. (l:Line {name: "test"}) or (l:Line) if name field doesn't exist
   */
  def toSearchObject(variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    fieldsPairs.get("name").map { name =>
      constructObject(constructFields(Map("name" -> name)), variableSuffixOverride)
    }.getOrElse(toAnyObjectOfType(variableSuffixOverride))
  }

  /**
   * Gets object of a graph element with just its type. Used to find all objects of that type.
   * e.g. (l:Line)
   */
  def toAnyObjectOfType(variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    constructObject(c"", variableSuffixOverride)
  }

  /**
   * Gets variable of a graph element.
   * e.g. l
   */
  def toVariable(variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    c"" + s"$variable$variableSuffixOverride"
  }

  /**
   * Gets variable enclosed in and object type of a graph element.
   * e.g. (l)
   */
  def toVariableEnclosed(variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    c"" + s"${elementClass.elementType.openMark}$variable$variableSuffixOverride${elementClass.elementType.endMark}"
  }

  /**
   * Gets variable enclosed in and object type of a graph element.
   * e.g. (l*1..4)
   */
  def toVariableEnclosedWithCardinality(
      cardinalityFrom: Option[Int],
      cardinalityTo: Option[Int],
      variableSuffixOverride: String = ""
  ): DeferredQueryBuilder = {
    val from = cardinalityFrom.map(f => s"$f..").getOrElse("..")
    val fromAndTo = cardinalityTo.map(t => s"$from$t").getOrElse(from)
    c"" + s"${elementClass.elementType.openMark}$variable$variableSuffixOverride*$fromAndTo${elementClass.elementType.endMark}"
  }

  /**
   * Gets variable with the specified field.
   * e.g. l.name
   */
  def toVariableWithField(fieldName: String, variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    if (fieldsPairs.contains(fieldName)) {
      c"" + s"$variable$variableSuffixOverride.$fieldName"
    } else {
      throw new IllegalAccessException(s"Field name $fieldName of the ${elementClass.name} doesn't exist!")
    }
  }

  /**
   * Gets variable with the specified field even if the field is not defined.
   * e.g. l.name
   */
  def toVariableWithNewField(fieldName: String, variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    c"" + s"$variable$variableSuffixOverride.$fieldName"
  }

  /**
   * Gets fields a graph element.
   * e.g. {name: "test", owner: "John Doe"}
   */
  def fields(): DeferredQueryBuilder = {
    constructFields(fieldsPairs)
  }

  private def constructObject(pairs: DeferredQueryBuilder, variableSuffixOverride: String = ""): DeferredQueryBuilder = {
    c"" +
      s"${elementClass.elementType.openMark}$variable$variableSuffixOverride:${elementClass.name}" +
      pairs +
      s"${elementClass.elementType.endMark}"
  }

  private def constructAnyClassObject(pairs: DeferredQueryBuilder, variableSuffixOverride: String = ""): DeferredQueryBuilder = {
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
