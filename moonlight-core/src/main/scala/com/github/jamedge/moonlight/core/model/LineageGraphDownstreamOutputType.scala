package com.github.jamedge.moonlight.core.model

sealed class LineageGraphDownstreamOutputType(val name: String)
object LineageGraphDownstreamOutputType {
  case object Json extends LineageGraphDownstreamOutputType("json")
  case object Md extends LineageGraphDownstreamOutputType("md")
}
