package com.github.jamedge.moonlight.api.versioning

sealed class FormattedOutputType(val name: String)
object FormattedOutputType {
  case object Json extends FormattedOutputType("json")
  case object Md extends FormattedOutputType("md")
  case object HTML extends FormattedOutputType("html")
}
