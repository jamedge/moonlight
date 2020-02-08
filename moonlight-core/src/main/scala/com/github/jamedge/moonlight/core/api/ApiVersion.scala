package com.github.jamedge.moonlight.core.api

import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Route
import com.github.jamedge.moonlight.core.api.handlers.ErrorResponseBuilder

case class VersionMapping(apiVersion: ApiVersion, route: Route)

sealed class ApiVersion(val version: Int) {
  val contentTypeMediaTypeValue: String =
  s"${ApiVersion.contentMediaTypeValuePrefix}$version${ApiVersion.contentMediaTypeValueSufix}"
}

object ApiVersion {
  private val contentMediaTypeValuePrefix = "application/moonlight.v"
  private val contentMediaTypeValueSufix = "+json"

  private def contentTypeMediaTypeFromatDescription: String =
  s"$contentMediaTypeValuePrefix<version_number>$contentMediaTypeValueSufix"

  def defaultErrorResponse(mediaType: MediaType): Route =
    ErrorResponseBuilder.buildErrorResponse(
    BadRequest,
    s"Content-Type header must be provided in a format '$contentTypeMediaTypeFromatDescription'." +
    s" Sent value '${mediaType.toString()}' is not recognised or not valid.")

  def mapVersion(mediaType: MediaType, mappings: Seq[VersionMapping]): Route = {
    mappings.find(_.apiVersion.contentTypeMediaTypeValue == mediaType.toString()).map(_.route).getOrElse(ApiVersion.defaultErrorResponse(mediaType))
  }

  case object V1 extends ApiVersion(1)
}

