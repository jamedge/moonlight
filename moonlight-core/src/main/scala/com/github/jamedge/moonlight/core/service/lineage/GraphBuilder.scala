package com.github.jamedge.moonlight.core.service.lineage

import com.github.jamedge.moonlight.core.model.{IOElement, Storage}
import org.neo4j.driver.v1.{Value, Values}
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import scalax.collection.io.json.{Descriptor, NodeDescriptor}
import scalax.collection.io.json.descriptor.predefined.LDi

object GraphBuilder {
  case class RawEdge(left: IOElement, properties: List[String], right: IOElement)

  def buildLineageGraph(
      rawEdges: List[RawEdge],
      rawIODetails: Map[String, Value],
      storages: Map[String, Storage],
      rawStorageDetails: Map[String, Value]
  ): Graph[IOElement, LDiEdge] = {
    val edges = rawEdges.map { rawEdge =>
      val left = buildIOElement(rawEdge.left, rawIODetails, storages, rawStorageDetails)
      val right = buildIOElement(rawEdge.right, rawIODetails, storages, rawStorageDetails)
      LDiEdge(left, right)(rawEdge.properties)
    }
    Graph[IOElement, LDiEdge](edges: _*)
  }

  private def buildIOElement(
      ioElement: IOElement,
      rawIODetails: Map[String, Value],
      storages: Map[String, Storage],
      rawStorageDetails: Map[String, Value]
  ): IOElement = {
    ioElement.copy(
      storage = storages.get(ioElement.name).map(s => s.copy(
        details = extractDetailsFromGraphValues(rawStorageDetails, s.name))),
      details = extractDetailsFromGraphValues(rawIODetails, ioElement.name))
  }

  private def extractDetailsFromGraphValues(values: Map[String, Value], parentName: String): Option[Map[String, String]] = {
    import scala.jdk.CollectionConverters._
    values.get(parentName).map(_.asMap[String](Values.ofString()).asScala.toMap)
  }

  def createLineageGraphJsonDescriptor(): Descriptor[IOElement] = {
    val ioDescriptor = new NodeDescriptor[IOElement](typeId = "IOs") {
      def id(node: Any): String = node match {
        case IOElement(name, owner, purpose, notes, details, storage, locationRelativePath) => name
      }
    }
    new Descriptor[IOElement](
      defaultNodeDescriptor = ioDescriptor,
      defaultEdgeDescriptor = LDi.descriptor[IOElement, String]("lines"),
      namedNodeDescriptors = Seq(ioDescriptor),
      namedEdgeDescriptors = Seq(LDi.descriptor[IOElement, String]("lines"))
    )
  }
}
