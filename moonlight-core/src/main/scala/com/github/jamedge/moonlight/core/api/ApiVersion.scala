package com.github.jamedge.moonlight.core.api

import akka.http.scaladsl.model.{HttpRequest, MediaType}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Route
import com.github.jamedge.moonlight.core.api.handlers.ErrorResponseBuilder

case class VersionRouteMapping(apiVersion: ApiVersion, route: Route)

sealed class ApiVersion(val version: Int) {
  val acceptValue: String =
  s"${ApiVersion.acceptValuePrefix}$version${ApiVersion.acceptValueSuffix}"
}

object ApiVersion { // TODO: use akka http content negotiation
  private val acceptValuePrefix = "application/moonlight.v"
  private val acceptValueSuffix = "+json"

  private def acceptFormatDescription: String =
    s"$acceptValuePrefix<version_number>$acceptValueSuffix"

  def defaultErrorResponse(accept: String): Route =
    ErrorResponseBuilder.buildErrorResponse(
    BadRequest,
    s"Accept header must be provided in a format '$acceptFormatDescription'." +
    s" Sent value '$accept' is not recognised or not valid.")

  implicit class VersionMappingOps(request: HttpRequest) {
    def matchApiVersion(versionRouteMappings: Seq[VersionRouteMapping]): Route = {
      val acceptHeader = request.getHeader("Accept")
      val acceptValue = if (acceptHeader.isPresent) acceptHeader.get().value() else ""
      versionRouteMappings.
        find(_.apiVersion.acceptValue == acceptValue).
        map(_.route).
        getOrElse(ApiVersion.defaultErrorResponse(acceptValue))
    }
  }

  case object V1 extends ApiVersion(1)
}

