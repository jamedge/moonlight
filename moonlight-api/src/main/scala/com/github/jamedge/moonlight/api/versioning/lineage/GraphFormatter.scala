package com.github.jamedge.moonlight.api.versioning.lineage

import com.github.jamedge.moonlight.api.ApiConfig
import com.github.jamedge.moonlight.api.versioning.FormattedOutputType.{HTML, Json, Md}
import com.github.jamedge.moonlight.api.versioning.{FormattedOutputType, HTMLGenerator}
import com.github.jamedge.moonlight.core.model.IOElement
import com.github.jamedge.moonlight.core.service.lineage.LineageGraph
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

import scala.util.Try

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

    val lineage = formatLineageGraph(lineageGraph)
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
      case HTML => s"[${elementName}](/$elementType/$elementName)"
      case Md => s"[$linkPrefix:${elementName}](#$linkPrefix:$elementName)"
      case Json | _ => elementName
    }
  }

  private def formatLineageGraph(
      lineageGraph: LineageGraph
  )(implicit outputConfig: OutputConfig.Downstream, outputType: FormattedOutputType): String = {
    val resultTry = for {
      root <- Try(lineageGraph.graph.nodes.find(_.toOuter.name == lineageGraph.rootNodeName).
        getOrElse(throw NonExistentRootException()))
      lineageGraphString <- Try(traverseGraph(lineageGraph.graph)(root, None, root, 0))
    } yield lineageGraphString
    resultTry.recover {
      case e: NonExistentRootException => generateEmptyLineage(lineageGraph.rootNodeName, outputConfig.emptyMessage)
      case e: CyclicLineageGraphException => generateEmptyLineage(lineageGraph.rootNodeName, outputConfig.cyclicMessage)
      case e: Exception => throw e
    }.get
  }

  /**
   * Function that traverses the input graph in a recursive matter and returns the appropriate
   * string representation of the whole traverse path.
   * @param ioGraph Graph to be traversed.
   * @param root Root node from which the traversal should start.
   * @param previousNode The last node being traversed outside this function.
   * @param masterRoot The node from which traversing was firstly initiated. It's used
   *                   to distinguish the currently running root node from the originating
   *                   traversal start in case when this function is recursively called.
   * @param depth Current recursion depth level of the traversal.
   * @param outputConfig Config containing the details needed to format output.
   * @param outputType Type of the output.
   * @return String representation of the traversal based on the output type.
   */
  private def traverseGraph(ioGraph: Graph[IOElement, LDiEdge])(
      root: ioGraph.NodeT,
      previousNode: Option[ioGraph.NodeT],
      masterRoot: ioGraph.NodeT,
      depth: Int
  )(implicit outputConfig: OutputConfig.Downstream, outputType: FormattedOutputType): String = {
    if (ioGraph.isCyclic) {
      throw CyclicLineageGraphException()
    }

    val opening = downstreamCaptionMainEnclosureOpen(ioGraph)(root, masterRoot, previousNode, depth) +
      downstreamCaptionNode(ioGraph)(root, masterRoot) +
      downstreamCaptionElementsSeparator(ioGraph)(root, masterRoot) +
      downstreamCaptionLines(ioGraph)(root, masterRoot, previousNode) +
      downstreamCaptionElementsSeparator(ioGraph)(root, masterRoot) +
      downstreamCaptionChildrenEnclosureOpen

    val children =
      if (root.hasSuccessors) {
        root.diSuccessors.toList.sortBy(_.name).map { successor =>
          traverseGraph(ioGraph)(successor, Some(root), masterRoot, depth + 1)
        }.mkString(downstreamCaptionElementsSeparator(ioGraph)(root, masterRoot))
      } else {
        ""
      }

    val closing = downstreamCaptionChildrenEnclosureClose +
      downstreamCaptionMainEnclosureClose(ioGraph)(root, masterRoot)

    opening + children + closing
  }

  private def downstreamCaptionMainEnclosureOpen(ioGraph: Graph[IOElement, LDiEdge])(
      currentNode: ioGraph.NodeT,
      root: ioGraph.NodeT,
      previousNode: Option[ioGraph.NodeT],
      currentLevel: Int
  )(implicit config: OutputConfig.Downstream): String = {
    if (currentNode eq root) {
      config.nodes.root.shell.prepend + config.nodes.root.shell.enclosure.start
    } else {
      config.newline +
        config.space * config.indentSize * currentLevel +
        (if (previousNode.isEmpty) config.nodes.children.separator else "") +
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
      previousNode: Option[ioGraph.NodeT]
  )(implicit config: OutputConfig.Downstream, outputType: FormattedOutputType): String = {
    val openString = if (currentNode eq root) {
      config.nodes.root.lines.prepend + config.nodes.root.lines.enclosure.start
    } else {
      config.nodes.children.element.lines.prepend + config.nodes.children.element.lines.enclosure.start
    }
    val linesString = previousNode.map { previous =>
      currentNode.
        connectionsWith(previous).
        flatMap(_.toOuter.label.asInstanceOf[List[String]].map(lineName =>
          generateElementLink(lineName, "line", "l")
        )).
        mkString(", ")
    }.getOrElse("")
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
case class NonExistentRootException() extends Exception
case class CyclicLineageGraphException() extends Exception
