package com.github.jamedge.moonlight.core.model.neo4j

import neotypes.DeferredQueryBuilder

import neotypes.implicits.all._

object GraphElementUtils {
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
      // TODO: think of another way how to make variables in a query uniquely assigned to a query element with same value each time
      // Commented the line below to make use of Neo4j caching functionality thus drastically improving query performance
      // The uniqueness of names for graph element variables is left for not to the developer to override in queries with new override parameters in GraphElement
//      s"${prefix}_${java.util.UUID.randomUUID.toString.replace("-", "")}"
      prefix
    } else {
      ""
    }
  }
}
