package com.github.jamedge.moonlight.core.api.versioning.lineage

sealed class LineageGraphFormattedOutputType(val name: String)
object LineageGraphFormattedOutputType { // TODO: add and use HTML output type
  case object Json extends LineageGraphFormattedOutputType("json")
  case object Md extends LineageGraphFormattedOutputType("md")
}
