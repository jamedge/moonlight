package com.github.jamedge.moonlight.api.versioning.lineage

import com.github.jamedge.moonlight.api.ApiConfig
import com.github.jamedge.moonlight.api.versioning.FormattedOutputType.{HTML, Md, Json}
import com.github.jamedge.moonlight.api.versioning.{FormattedOutputType, HTMLGenerator}
import com.github.jamedge.moonlight.core.model.IOElement
import com.github.jamedge.moonlight.core.service.fixture.LineageGraph
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

class GraphFormatter( // TODO: refactor this class to better split reponsibilities and see what needs to be private and what not
    outputConfig: OutputConfig.Output,
    apiConfig: ApiConfig,
    htmlGenerator: HTMLGenerator
) {
  /**
   * Transforms the lineage graph made from downstream IO Elements and the specified root element
   * into the desired representation.
   *
   * @param lineageGraph Lineage graph made from downstream IO Elements.
   * @param outputType Desired format of output representation.
   * @return Graph formatted as a tree of IO Elements.
   */
  def formatLineageGraph(lineageGraph: LineageGraph, outputType: FormattedOutputType): String = {
    implicit val c = outputConfig.downstream(if (outputType.name == "html") "md" else outputType.name)
    implicit val ot = outputType

    val lineage = formatLineageGraph(lineageGraph).
      getOrElse(generateEmptyLineage(lineageGraph.rootNodeName, c.emptyMessage))

    if (outputType == HTML) {
      htmlGenerator.
        generateHTML(lineage).
        getOrElse(throw LineageHTMLGenerationException("Error generating lineage HTML!"))
    } else lineage
  }

  private def generateEmptyLineage(
      rootNodeName: String,
      emptyMessage: String
  )(implicit outputType: FormattedOutputType, outputConfig: OutputConfig.Downstream): String = {
    outputType match {
      case Md =>
        outputConfig.nodes.root.node.enclosure.start + rootNodeName + outputConfig.nodes.root.node.enclosure.end +
        downstreamCaptionChildrenEnclosureOpen +
        outputConfig.newline + outputConfig.space * outputConfig.indentSize * 2 +
        emptyMessage +
        downstreamCaptionChildrenEnclosureClose
      case HTML | Json | _ => emptyMessage
    }
  }

  private def generateElementLink(
      elementName: String,
      elementType: String,
      linkPrefix: String
  )(implicit outputType: FormattedOutputType): String = {
    outputType match {
      case HTML => s"[${elementName}](http://${apiConfig.server.host}:${apiConfig.server.port}/$elementType/$elementName)"
      case Md => s"[$linkPrefix:${elementName}](#$linkPrefix:$elementName)"
      case Json | _ => elementName
    }
  }

  private def formatLineageGraph(
      lineageGraph: LineageGraph
  )(implicit outputConfig: OutputConfig.Downstream, outputType: FormattedOutputType): Option[String] = {
    lineageGraph.graph.nodes.find(_.toOuter.name == lineageGraph.rootNodeName).map { root =>

      case class Accumulator(
          resultString: String,
          previousNodes: List[lineageGraph.graph.NodeT],
          previousNode: lineageGraph.graph.NodeT,
          level: Int)

      val emptyElement = IOElement("", None, None, None, None, None, None)
      val emptyAccumulator = Accumulator(
        "",
        List(lineageGraph.graph.Node(emptyElement)), lineageGraph.graph.Node(emptyElement), 0)

      val result = root.innerNodeDownUpTraverser.foldLeft(emptyAccumulator) { case (acc, (firstDip, currentNode)) =>
        if (firstDip) {
          val currentLevel = acc.level + 1
          Accumulator(
            resultString = acc.resultString +
              downstreamCaptionMainEnclosureOpen(lineageGraph.graph)(currentNode, root, acc.previousNode, currentLevel) +
              downstreamCaptionNode(lineageGraph.graph)(currentNode, root) +
              downstreamCaptionElementsSeparator(lineageGraph.graph)(currentNode, root) +
              downstreamCaptionLines(lineageGraph.graph)(currentNode, root, acc.previousNodes.head) +
              downstreamCaptionElementsSeparator(lineageGraph.graph)(currentNode, root) +
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
              downstreamCaptionMainEnclosureClose(lineageGraph.graph)(currentNode, root),
            previousNodes = acc.previousNodes.tail,
            previousNode = currentNode,
            level = currentLevel
          )
        }
      }
      result.resultString
    }
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
  )(implicit config: OutputConfig.Downstream, outputType: FormattedOutputType): String = {
    val openString = if (currentNode eq root) {
      config.nodes.root.lines.prepend + config.nodes.root.lines.enclosure.start
    } else {
      config.nodes.children.element.lines.prepend + config.nodes.children.element.lines.enclosure.start
    }
    val linesString = currentNode.
      connectionsWith(headingPreviousNode).
      flatMap(_.toOuter.label.asInstanceOf[List[String]].map(lineName =>
        generateElementLink(lineName, "line", "l")
      )).
      mkString(", ")
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

case class LineageHTMLGenerationException(message: String) extends Exception(message)
