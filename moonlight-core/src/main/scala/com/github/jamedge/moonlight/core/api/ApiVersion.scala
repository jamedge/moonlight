package com.github.jamedge.moonlight.core.api

import akka.http.scaladsl.model.{HttpRequest, MediaType}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Route
import com.github.jamedge.moonlight.core.api.handlers.ErrorResponseBuilder

case class VersionRouteMapping(apiVersion: ApiVersion, route: Route)

sealed class ApiVersion(val version: Int) {
  val contentTypeMediaTypeValue: String =
  s"${ApiVersion.contentMediaTypeValuePrefix}$version${ApiVersion.contentMediaTypeValueSuffix}"
}

object ApiVersion {
  private val contentMediaTypeValuePrefix = "application/moonlight.v"
  private val contentMediaTypeValueSuffix = "+json"

  private def contentTypeMediaTypeFormatDescription: String =
    s"$contentMediaTypeValuePrefix<version_number>$contentMediaTypeValueSuffix"

  def defaultErrorResponse(mediaType: MediaType): Route =
    ErrorResponseBuilder.buildErrorResponse(
    BadRequest,
    s"Content-Type header must be provided in a format '$contentTypeMediaTypeFormatDescription'." +
    s" Sent value '${mediaType.toString()}' is not recognised or not valid.")

  implicit class VersionMappingOps(request: HttpRequest) {
    def matchApiVersion(versionRouteMappings: Seq[VersionRouteMapping]): Route = {
      versionRouteMappings.
        find(_.apiVersion.contentTypeMediaTypeValue == request.entity.contentType.mediaType.toString()).
        map(_.route).
        getOrElse(ApiVersion.defaultErrorResponse(request.entity.contentType.mediaType))
    }
  }

  case object V1 extends ApiVersion(1)
}

