package com.github.jamedge.moonlight.api.versioning

import akka.http.scaladsl.model.{HttpCharsets, MediaType}

object MediaVersionTypes {
  def customApplicationMediaType(subType: String) =
    MediaType.customWithFixedCharset("application", subType, HttpCharsets.`UTF-8`)
  def customTextMediaType(subType: String) =
    MediaType.customWithFixedCharset("text", subType, HttpCharsets.`UTF-8`)

  val `application/moonlight.v1+json`: MediaType.WithFixedCharset =
    customApplicationMediaType("moonlight.v1+json")
  val `text/moonlight.v1+html`: MediaType.WithFixedCharset =
    customTextMediaType("moonlight.v1+html")
  val `text/moonlight.v1+markdown`: MediaType.WithFixedCharset =
    customTextMediaType("moonlight.v1+markdown")
}
