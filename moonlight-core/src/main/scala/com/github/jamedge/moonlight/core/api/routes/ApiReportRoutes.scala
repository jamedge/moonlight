package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.{Directives, MalformedHeaderRejection, Route}
import com.github.jamedge.moonlight.core.api.handlers.ApiException
import com.github.jamedge.moonlight.core.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.core.api.versioning.line.LineHTMLSupport.generateLineHTML
import com.github.jamedge.moonlight.core.api.versioning.line.{LineMDGenerator, LineRoutesSupport, LineV1}
import com.github.jamedge.moonlight.core.service.line.LineService

import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, ExecutionContext, Future}

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
        headerValueByType[Accept]() { accept =>
          if (accept.mediaRanges.exists(_.matches(MediaTypes.`text/html`))) {
            complete(HttpResponse(
              StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, Await.result(getLinesHtml(), Inf)) // TODO: make this duration configurable
            ))
          } else {
            reject(MalformedHeaderRejection("Accept", s"Provided value: ${accept.value()}. Supported values: 'text/html'.", None))
          }
        }
      }
    }
  }

  private def getLinesHtml(): Future[String] = {
    for {
      lines <- lineService.getLines
      bodyString <- Future(lines.map(line => generateLineHTML(LineV1.toLineV1(line))).mkString("<br>"))
      headerString <- Future(generateHeaderHtml)
      footerString <- Future(generateFooterHtml)
      htmlString <- Future(headerString + bodyString + footerString) // TODO: think about table of contents
    } yield htmlString
  }

  private def generateHeaderHtml: String = {
    "<p>List of all entered lines to the database:</p>" // TODO: make it more meaningful
  }

  private def generateFooterHtml: String = {
    "<p>Generated by `moonlight`.</p>" // TODO: make it more meaningful
  }
}

case class UnsupportedAcceptException(acceptValue: String)
  extends ApiException(
    new Exception(s"Unsupported value for header 'Accept': $acceptValue. Value should be 'text/html'."),
    StatusCodes.BadRequest,
    Some(s"Unsupported value for header 'Accept': $acceptValue. Value should be 'text/html'.")
  )
