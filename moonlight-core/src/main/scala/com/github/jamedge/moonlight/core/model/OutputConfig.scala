package com.github.jamedge.moonlight.core.model

object OutputConfig {
  case class Output(downstream: Map[String, Downstream])
  case class Downstream(nodes: DownstreamElements, newline: String, space: String, indentSize: Int, emptyMessage: String)
  case class DownstreamElements(root: DownstreamElement, children: DownstreamListElement)
  case class DownstreamElement(shell: DownstreamElementView, node: DownstreamElementView, lines: DownstreamElementView, separator: String)
  case class DownstreamListElement(shell: DownstreamElementView, element: DownstreamElement, separator: String)
  case class DownstreamElementView(prepend: String, enclosure: Enclosure)
  case class Enclosure(start: String, end: String)
}

