package com.github.jamedge.moonlight.api.versioning.lineage

import com.github.jamedge.moonlight.api.Module
import com.github.jamedge.moonlight.api.versioning.FormattedOutputType.Json
import com.github.jamedge.moonlight.core.model.IOElement
import com.github.jamedge.moonlight.core.service.fixture.{GraphBuilder, IOElementFixture, LineageGraph}
import com.github.jamedge.moonlight.core.service.fixture.GraphBuilder.RawEdge
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

class GraphFormatterSpec extends AnyFunSpec with Matchers {
  val module = new Module("moonlight-api")

  val subject: GraphFormatter = module.lineageGraphFormatter

  val testIOPool = new IOElementFixture(1, 6)

  val testGraphEdges: List[RawEdge] = List(
    RawEdge(testIOPool.get(1).get, List("test_line1"), testIOPool.get(2).get),
    RawEdge(testIOPool.get(1).get, List("test_line1"), testIOPool.get(3).get),
    RawEdge(testIOPool.get(1).get, List("test_line1"), testIOPool.get(4).get),
    RawEdge(testIOPool.get(2).get, List("test_line2"), testIOPool.get(5).get),
    RawEdge(testIOPool.get(2).get, List("test_line2"), testIOPool.get(6).get),
    RawEdge(testIOPool.get(1).get, List("test_line1", "test_line2"), testIOPool.get(5).get),
    RawEdge(testIOPool.get(1).get, List("test_line1", "test_line2"), testIOPool.get(6).get),
  )
  val testGraph: Graph[IOElement, LDiEdge] = GraphBuilder.buildLineageGraph(
    testGraphEdges,
    Map(),
    testIOPool.storageFixture.pool,
    Map()
  )

  implicit val formats = DefaultFormats

  describe("#formatLineageGraph") {
    it ("should return json containing the root node and all of its children when" +
      "it belongs to graph and has decedents") {
      val testLineageGraph = LineageGraph(testIOPool.get(1).get.name, testGraph)
      val result = subject.formatLineageGraph(testLineageGraph, Json)
      val resultJsonLinesTree = read[JsonLinesTree](result)

      resultJsonLinesTree.name shouldBe "test_io_element_1"
      resultJsonLinesTree.lines shouldBe ""
      resultJsonLinesTree.children should have size 5

      resultJsonLinesTree.children(0).name shouldBe "test_io_element_2"
      resultJsonLinesTree.children(0).lines shouldBe "test_line1"
      resultJsonLinesTree.children(0).children should have size 2
      resultJsonLinesTree.children(0).children should contain (JsonLinesTree("test_io_element_5", "test_line2", List()))
      resultJsonLinesTree.children(0).children should contain (JsonLinesTree("test_io_element_6", "test_line2", List()))

      resultJsonLinesTree.children(1).name shouldBe "test_io_element_3"
      resultJsonLinesTree.children(1).lines shouldBe "test_line1"
      resultJsonLinesTree.children(1).children shouldBe empty

      resultJsonLinesTree.children(2).name shouldBe "test_io_element_4"
      resultJsonLinesTree.children(2).lines shouldBe "test_line1"
      resultJsonLinesTree.children(1).children shouldBe empty

      resultJsonLinesTree.children(3).name shouldBe "test_io_element_5"
      resultJsonLinesTree.children(3).lines shouldBe "test_line1, test_line2"
      resultJsonLinesTree.children(1).children shouldBe empty

      resultJsonLinesTree.children(4).name shouldBe "test_io_element_6"
      resultJsonLinesTree.children(4).lines shouldBe "test_line1, test_line2"
      resultJsonLinesTree.children(1).children shouldBe empty
    }
  }

  it ("should return json containing just the root node when it belongs to graph but has no decedents") {
    val testLineageGraph = LineageGraph(testIOPool.get(3).get.name, testGraph)
    val result = subject.formatLineageGraph(testLineageGraph, Json)
    val resultInfoMessage = read[JsonLinesTree](result)

    resultInfoMessage shouldBe JsonLinesTree("test_io_element_3", "", List())
  }

  it ("should return an empty message when the root doesn't belongs to the graph") {
    val testLineageGraph = LineageGraph("nonexistant_root", testGraph)
    val result = subject.formatLineageGraph(testLineageGraph, Json)
    val resultInfoMessage = read[InfoMessage](result)

    resultInfoMessage.info shouldBe "This IO has no outputs."
  }
}

case class JsonLinesTree(name: String, lines: String, children: List[JsonLinesTree])
case class InfoMessage(info: String)
