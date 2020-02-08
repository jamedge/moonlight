package com.github.jamedge.moonlight.core.api

sealed class ApiVersion(val contentTypeMediaTypeValue: String)
object ApiVersion {
  case object V1 extends ApiVersion("application/moonlight.v1+json")
}
