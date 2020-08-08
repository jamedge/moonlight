package com.github.jamedge.moonlight.api.versioning.lineage

sealed class LineageGraphFormattedOutputType(val name: String)
object LineageGraphFormattedOutputType {
  case object Json extends LineageGraphFormattedOutputType("json")
  case object Md extends LineageGraphFormattedOutputType("md")
  case object HTML extends LineageGraphFormattedOutputType("html")
}
