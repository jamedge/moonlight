package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer
import com.github.jamedge.moonlight.core.api.versioning.line.LineJsonSupport
import com.github.jamedge.moonlight.core.api.versioning.responsemessage.ResponseMessageJsonSupport
import com.github.jamedge.moonlight.core.model.Line
import com.github.jamedge.moonlight.core.service.line.LineService

import scala.concurrent.ExecutionContext

case class ResponseMessage(message: String)

trait LineRoutesJsonSupport {
  implicit def lineUnmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    LineJsonSupport.unmarshaller
  implicit def lineMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[Line] =
    LineJsonSupport.marshaller
  implicit def ResponseMessageMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[ResponseMessage] =
    ResponseMessageJsonSupport.marshaller
}

class ApiLineRoutes(
    lineService: LineService
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem)
  extends Directives with LineRoutesJsonSupport {

  def routes: Route = {
    line
  }

  private[routes] def line: Route = {
    path("line") {
      post {
        entity(as[Line]) { line =>
          complete(lineService.addLine(line).map(_ => line))//ResponseMessage("Line added/updated.")))
        }
      } ~
      get { // TODO: move to path with path param for line name
        complete(ResponseMessage("Test getting of line successful!"))
      }
    }
  }
}
