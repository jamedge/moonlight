package com.github.jamedge.moonlight.core.api.versioning

import akka.http.scaladsl.model.{HttpCharsets, MediaType}

object MediaVersionTypes {
  def customMediaType(subType: String) =
    MediaType.customWithFixedCharset("application", subType, HttpCharsets.`UTF-8`)

  val `application/moonlight.v1+json`: MediaType.WithFixedCharset = customMediaType("moonlight.v1+json")
}
