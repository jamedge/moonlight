package com.github.jamedge.moonlight.core.api

import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Route
import com.github.jamedge.moonlight.core.api.handlers.ErrorResponseBuilder

sealed class ApiVersion(val version: Int) {

  private val contentMediaTypeValuePrefix = "application/moonlight.v"
  private val contentMediaTypeValueSufix = "+json"

  def contentTypeMediaTypeValue: String =
    s"$contentMediaTypeValuePrefix$version$contentMediaTypeValueSufix"

  private def contentTypeMediaTypeFromatDescription: String =
    s"$contentMediaTypeValuePrefix<version_number>$contentMediaTypeValueSufix"

  def checkMediaType(mediaType: MediaType, route: Route): Route = {
    if (contentTypeMediaTypeValue == mediaType.toString()) {
      route
    } else {
      ErrorResponseBuilder.buildErrorResponse(
        BadRequest,
        s"Content-Type header must be provided in a format '$contentTypeMediaTypeFromatDescription'." +
          s" Sent value '$mediaType' is not recognised or not valid.")
    }
  }
}
object ApiVersion {
  case object V1 extends ApiVersion(1)
}
