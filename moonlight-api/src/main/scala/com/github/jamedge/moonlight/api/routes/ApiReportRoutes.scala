package com.github.jamedge.moonlight.api.routes

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.{Directives, MalformedHeaderRejection, Route}
import com.github.jamedge.moonlight.api.ApiConfig
import com.github.jamedge.moonlight.api.versioning.line.LineRoutesSupport
import com.github.jamedge.moonlight.api.versioning.report.ReportGenerator

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class ApiReportRoutes(
    apiConfig: ApiConfig,
    reportGenerator: ReportGenerator
)(
    implicit val executionContext: ExecutionContext,
    actorSystem: ActorSystem,
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
              entity = HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                Await.result(reportGenerator.getLinesHtml(), apiConfig.routesLoadTimeoutsMs.report.milliseconds))
            ))
          } else if (accept.mediaRanges.exists(_.matches(MediaTypes.`text/markdown`))) {
            complete(HttpResponse(
              StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                Await.result(reportGenerator.getLinesMarkdown(), apiConfig.routesLoadTimeoutsMs.report.milliseconds))))
          } else {
            reject(MalformedHeaderRejection("Accept", s"Provided value: ${accept.value()}. Supported values: 'text/html'.", None))
          }
        }
      }
    }
  }
}
