package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.javadsl.model.HttpCharsets
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, HttpResponse, MediaRange, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import com.github.jamedge.moonlight.core.api.handlers.ApiException
import com.github.jamedge.moonlight.core.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.core.api.versioning.line.{LineMDGenerator, LineRoutesSupport}
import com.github.jamedge.moonlight.core.service.line.LineService

import scala.concurrent.ExecutionContext

class ApiReportRoutes(
    lineService: LineService
) (
    implicit val executionContext: ExecutionContext,
    actorSystem: ActorSystem,
    lineMDGenerator: LineMDGenerator,
    htmlGenerator: HTMLGenerator
) extends Directives with LineRoutesSupport {

  def routes: Route = {
    report
  }

  private[routes] def report: Route = {
    path("report") {
      get {
        headerValueByName("Accept") { acceptValue =>
          if (acceptValue == "text/html" | acceptValue == "*/*") {
            complete(HttpResponse(
              StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, "<p>Test<p>")
            ))
          } else {
            throw UnsupportedAcceptException(acceptValue)
          }
        }
      }
    }
  }
}

case class UnsupportedAcceptException(acceptValue: String)
  extends ApiException(
    new Exception(s"Unsupported value for header 'Accept': $acceptValue. Value should be 'text/html'."),
    StatusCodes.BadRequest,
    Some(s"Unsupported value for header 'Accept': $acceptValue. Value should be 'text/html'.")
  )
