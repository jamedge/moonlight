package com.github.jamedge.moonlight.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import com.github.jamedge.moonlight.api.handlers.ApiException
import com.github.jamedge.moonlight.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.api.versioning.line.{LineMDGenerator, LineRoutesSupport}
import com.github.jamedge.moonlight.core.service.line.LineService
import com.github.jamedge.moonlight.core.model.Line

import scala.concurrent.ExecutionContext

class ApiLineRoutes(
    lineService: LineService
) (
    implicit val executionContext: ExecutionContext,
    actorSystem: ActorSystem,
    lineMDGenerator: LineMDGenerator,
    htmlGenerator: HTMLGenerator
) extends Directives with LineRoutesSupport {

  def routes: Route = {
    line ~
    lines
  }

  private[routes] def line: Route = {
    path("line") {
      post {
        entity(as[Line]) { line =>
          complete(lineService.addLine(line).map(_ => ResponseMessage("Line added/updated.")))
        }
      }
    } ~
    path("line" / Segment) { lineName =>
      get {
        complete(lineService.getLine(lineName).map(_.getOrElse {
          val message = s"""Line with name "$lineName" doesn't exist."""
          throw new ApiException(new Exception(message), StatusCodes.BadRequest, Some(message))}))
      }
    }
  }

  private[routes] def lines: Route = {
    path("lines") {
      get {
        complete(lineService.getLines)
      }
    }
  }
}
