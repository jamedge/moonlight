package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer
import com.github.jamedge.moonlight.core.api.handlers.ApiException
import com.github.jamedge.moonlight.core.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.core.api.versioning.line.{LineHTMLSupport, LineJsonSupport, LineMDGenerator, LineMDSupport}
import com.github.jamedge.moonlight.core.api.versioning.responsemessage.ResponseMessageJsonSupport
import com.github.jamedge.moonlight.core.model.Line
import com.github.jamedge.moonlight.core.service.line.LineService
import scala.concurrent.duration._

import scala.concurrent.{Await, ExecutionContext}

case class ResponseMessage(message: String)

trait LineRoutesSupport {
  implicit def lineUnmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    LineJsonSupport.unmarshaller
  implicit def lineMarshaller(
      implicit ec: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      htmlGenerator: HTMLGenerator
  ): ToResponseMarshaller[Line] =
    Marshaller.oneOf(
      LineJsonSupport.marshaller,
      LineHTMLSupport.marshaller,
      LineMDSupport.marshaller
    )
  implicit def ResponseMessageMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[ResponseMessage] =
    ResponseMessageJsonSupport.marshaller
}

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
        complete(Await.result(lineService.getLines, 60.seconds).toString) // TODO: add marshaller for List[Line]
      }
    }
  }
}
