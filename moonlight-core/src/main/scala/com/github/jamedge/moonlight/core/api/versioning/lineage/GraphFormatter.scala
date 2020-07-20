package com.github.jamedge.moonlight.core.api.versioning.lineage

import com.github.jamedge.moonlight.core.api.ApiConfig
import com.github.jamedge.moonlight.core.model.IOElement
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

class GraphFormatter(
    outputConfig: OutputConfig.Output,
    apiConfig: ApiConfig
) {
  /**
   * Transforms the lineage graph made from downstream IO Elements and the specified root element
   * into the desired representation.
   *
   * @param ioGraph Lineage graph made from downstream IO Elements.
   * @param rootIOElementName Value of the `name` attribute of the root IO Element.
   * @param outputType Desired format of output representation.
   * @return Graph formatted as a tree of IO Elements.
   */
  def formatLineageGraph(ioGraph: Graph[IOElement, LDiEdge], rootIOElementName: String, outputType: LineageGraphFormattedOutputType): String = {
    implicit val c = outputConfig.downstream(outputType.name)
    implicit val ot = outputType
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
  )(implicit config: OutputConfig.Downstream, outputType: LineageGraphFormattedOutputType): String = {
    val openString = if (currentNode eq root) {
      config.nodes.root.lines.prepend + config.nodes.root.lines.enclosure.start
    } else {
      config.nodes.children.element.lines.prepend + config.nodes.children.element.lines.enclosure.start
    }
    val linesString = currentNode.
      connectionsWith(headingPreviousNode).
      flatMap(_.toOuter.label.asInstanceOf[List[String]].map(lineName =>
        if (outputType == LineageGraphFormattedOutputType.Md)
          s"[${lineName}](http://${apiConfig.server.host}:${apiConfig.server.port}/line/$lineName)" // TODO: consider of alternative link for md generation while keepping this one for HTML gen
        else lineName
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
