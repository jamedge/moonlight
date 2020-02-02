package com.github.jamedge.moonlight.core.service

import com.github.jamedge.moonlight.core.model.{IOElement, Storage}
import com.github.jamedge.moonlight.core.service.neo4j.LineageQueries
import neotypes.{Driver, Transaction}
import org.neo4j.driver.v1.{Value, Values}
import org.slf4j.Logger
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import scalax.collection.io.json.{Descriptor, NodeDescriptor}
import scalax.collection.io.json.descriptor.predefined.LDi
import shapeless.Id
import neotypes.implicits.all._

import scala.concurrent.{ExecutionContext, Future}

class LineageService(
    neo4jDriver: Id[Driver[Future]],
    logger: Logger,
    outputConfig: OutputConfig.Output
)(implicit val executionContext: ExecutionContext) {
  case class RawEdge(left: IOElement, properties: List[String], right: IOElement)

  /**
   * Gets json representation of the lineage graph fetched from the persistence layer.
   * <br/><br/>
   * Retrieves graph of downstream IO Elements for the specified root element.
   *
   * @param rootIOElementName Value of the `name` attribute of the root IO Element.
   * @return Graph containing all IO Elements downstream from the root IO Element.
   */
  def getLineageGraphJson(rootIOElementName: String): Future[String] = {
    import scalax.collection.io.json._
    for {
      graph <- getLineageGraph(rootIOElementName)
      resultJson <- Future(graph.toJson(createLineageGraphJsonDescriptor()))
    } yield resultJson
  }

  protected def getLineageGraph(rootIOElementName: String): Future[Graph[IOElement, LDiEdge]] = {
    neo4jDriver.readSession { implicit session =>
      session.transact[Graph[IOElement, LDiEdge]] { implicit tx =>
        for {
          rawEdges <- LineageQueries.constructGetLineageRawEdgesQuery(rootIOElementName).query[RawEdge].list(tx)
          rawIODetails <- LineageQueries.constructGetAllIODetailsQuery().query[(String, Value)].map(tx)
          storages <- LineageQueries.constructGetAllIOStoragesQuery().query[(String, Storage)].map(tx)
          rawStorageDetails <- LineageQueries.constructGetAllStorageDetailsQuery().query[(String, Value)].map(tx)
          graph <- Future(buildLineageGraph(rawEdges, rawIODetails, storages, rawStorageDetails))
        } yield graph
      }
    }
  }

  private def buildLineageGraph(
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

  private def createLineageGraphJsonDescriptor(): Descriptor[IOElement] = {
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

  /**
   * Gets json representation of the lineage graph fetched from the persistence layer.
   * <br/><br/>
   * Retrieves graph of downstream IO Elements for the specified root element.
   *
   * @param rootIOElementName Value of the `name` attribute of the root IO Element.
   * @param outputType Desired format of output representation (can be
   * @return Graph containing all IO Elements downstream from the root IO Element.
   */
  def getLineageGraphFormatted(rootIOElementName: String, outputType: LineageGraphFormattedOutputType): Future[String] = {
    for {
      graph <- getLineageGraph(rootIOElementName)
      resultJson <- Future(formatLineageGraph(graph, rootIOElementName, outputType))
    } yield resultJson
  }

  private def formatLineageGraph(ioGraph: Graph[IOElement, LDiEdge], rootIOElementName: String, outputType: LineageGraphFormattedOutputType): String = {
    implicit val c = outputConfig.downstream(outputType.name)
    ioGraph.nodes.find(_.toOuter.name == rootIOElementName).map { root =>

      case class Accumulator(resultString: String, previousNodes: List[ioGraph.NodeT], previousNode: ioGraph.NodeT, level: Int)
      val emptyElement = IOElement("", None, None, None, None, None, None)
      val emptyAccumulator = Accumulator("", List(ioGraph.Node(emptyElement)), ioGraph.Node(emptyElement), 0)

      val result = root.innerNodeDownUpTraverser.foldLeft(emptyAccumulator) { case (acc, (firstDip, currentNode)) =>
        if (firstDip) {
          val currentLevel = acc.level + 1
          Accumulator(
            resultString = acc.resultString +
              downstreamCaptionMainEnclosureOpen(ioGraph)(currentNode, root, acc.previousNode, currentLevel) +
              downstreamCaptionNode(ioGraph)(currentNode, root) +
              downstreamCaptionElementsSeparator(ioGraph)(currentNode, root) +
              downstreamCaptionLines(ioGraph)(currentNode, root, acc.previousNodes.head) +
              downstreamCaptionElementsSeparator(ioGraph)(currentNode, root) +
              downstreamCaptionChildrenEnclosureOpen,
            previousNodes = currentNode :: acc.previousNodes,
            previousNode = currentNode,
            level = currentLevel
          )
        }
        else {
          val currentLevel = acc.level - 1
          Accumulator(
            resultString = acc.resultString +
              downstreamCaptionChildrenEnclosureClose +
              downstreamCaptionMainEnclosureClose(ioGraph)(currentNode, root),
            previousNodes = acc.previousNodes.tail,
            previousNode = currentNode,
            level = currentLevel
          )
        }
      }
      result.resultString
    }.getOrElse(c.emptyMessage)
  }

  private def downstreamCaptionMainEnclosureOpen(ioGraph: Graph[IOElement, LDiEdge])(
      currentNode: ioGraph.NodeT,
      root: ioGraph.NodeT,
      previousNode: ioGraph.NodeT,
      currentLevel: Int
  )(implicit config: OutputConfig.Downstream): String = {
    if (currentNode eq root) {
      config.nodes.root.shell.prepend + config.nodes.root.shell.enclosure.start
    } else {
      config.newline +
        config.space * config.indentSize * currentLevel +
        (if (currentNode.connectionsWith(previousNode).isEmpty) config.nodes.children.separator else "") +
        config.nodes.children.element.shell.prepend +
        config.nodes.children.element.shell.enclosure.start
    }
  }

  private def downstreamCaptionMainEnclosureClose(ioGraph: Graph[IOElement, LDiEdge])(
      currentNode: ioGraph.NodeT,
      root: ioGraph.NodeT
  )(implicit config: OutputConfig.Downstream): String = {
    if (currentNode eq root) {
      config.nodes.root.shell.enclosure.end
    } else {
      config.nodes.children.element.shell.enclosure.end
    }
  }

  private def downstreamCaptionNode(ioGraph: Graph[IOElement, LDiEdge])(
      currentNode: ioGraph.NodeT,
      root: ioGraph.NodeT
  )(implicit config: OutputConfig.Downstream): String = {
    val openString = if (currentNode eq root) {
      config.nodes.root.node.prepend + config.nodes.root.node.enclosure.start
    } else {
      config.nodes.children.element.node.prepend + config.nodes.children.element.node.enclosure.start
    }
    val nodeString = currentNode.toOuter.name.toString
    val closeString = if (currentNode eq root) {
      config.nodes.root.node.enclosure.end
    } else {
      config.nodes.children.element.node.enclosure.end
    }
    openString + nodeString + closeString
  }

  private def downstreamCaptionLines(ioGraph: Graph[IOElement, LDiEdge])(
      currentNode: ioGraph.NodeT,
      root: ioGraph.NodeT,
      headingPreviousNode: ioGraph.NodeT
  )(implicit config: OutputConfig.Downstream): String = {
    val openString = if (currentNode eq root) {
      config.nodes.root.lines.prepend + config.nodes.root.lines.enclosure.start
    } else {
      config.nodes.children.element.lines.prepend + config.nodes.children.element.lines.enclosure.start
    }
    val linesString = currentNode.connectionsWith(headingPreviousNode).flatMap(_.toOuter.label.asInstanceOf[List[String]]).mkString(", ")
    val closeString = if (currentNode eq root) {
      config.nodes.root.lines.enclosure.end
    } else {
      config.nodes.children.element.lines.enclosure.end
    }
    openString + linesString + closeString
  }

  private def downstreamCaptionElementsSeparator(ioGraph: Graph[IOElement, LDiEdge])(
      currentNode: ioGraph.NodeT,
      root: ioGraph.NodeT
  )(implicit config: OutputConfig.Downstream): String = {
    if (currentNode eq root) {
      config.nodes.root.separator
    } else {
      config.nodes.children.element.separator
    }
  }

  private def downstreamCaptionChildrenEnclosureOpen(implicit config: OutputConfig.Downstream): String = {
    config.nodes.children.shell.prepend + config.nodes.children.shell.enclosure.start
  }

  private def downstreamCaptionChildrenEnclosureClose(implicit config: OutputConfig.Downstream): String = {
    config.nodes.children.shell.enclosure.end
  }
}
