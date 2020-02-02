package com.github.jamedge.moonlight.core.service

sealed class LineageGraphFormattedOutputType(val name: String)
object LineageGraphFormattedOutputType {
  case object Json extends LineageGraphFormattedOutputType("json")
  case object Md extends LineageGraphFormattedOutputType("md")
}
