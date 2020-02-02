package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directives, Route}
import com.github.jamedge.moonlight.core.model.Line
import com.github.jamedge.moonlight.core.service.line.LineService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read

import scala.concurrent.ExecutionContext

class ApiLineRoutes(
    lineService: LineService,
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem) extends Directives {

  implicit val formats = DefaultFormats

  def routes: Route = {
    post {
      path("line" / "add") {
        entity(as[String]) { requestBody =>
          val result = lineService.addLine(read[Line](requestBody)).map(r => "Line added/updated.")
          complete(result)
        }
      }
    }
  }
}
