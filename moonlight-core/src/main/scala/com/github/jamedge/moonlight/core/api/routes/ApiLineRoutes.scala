package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.{Directives, Route}
import com.fasterxml.jackson.core.JsonParseException
import com.github.jamedge.moonlight.core.api.ApiVersion
import com.github.jamedge.moonlight.core.api.handlers.{ApiException, ErrorResponseBuilder}
import com.github.jamedge.moonlight.core.model.Line
import com.github.jamedge.moonlight.core.service.line.LineService
import org.json4s.{DefaultFormats, MappingException}
import org.json4s.jackson.Serialization.read

import scala.concurrent.ExecutionContext
import scala.util.Try

class ApiLineRoutes(
    lineService: LineService
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem) extends Directives {

  implicit val formats = DefaultFormats

  def routes: Route = {
    post {
      path("line" / "add") {
        extractRequest { request =>
          val mediaType = request.entity.contentType.mediaType.toString()
          mediaType match {
            case ApiVersion.V1.contentTypeMediaTypeValue => addLineV1
            case _ => ErrorResponseBuilder.buildErrorResponse(
              BadRequest,
                s"Content-Type header must be provided in a defined format 'application/moonlight.v<version_number>+json'." +
                  s" Sent value '$mediaType' is not recognised or not valid.")
          }
        }
      }
    }
  }

  def addLineV1: Route = {
    entity(as[String]) { requestBody =>
      val result = for {
        line <- Try(read[Line](requestBody))
        lineAdditionResult <- Try(lineService.addLine(line).map(r => "Line added/updated."))
      } yield lineAdditionResult
      result.map(complete(_)).recover {
        case e@(_: MappingException | _: JsonParseException) =>
          throw new ApiException(e, BadRequest, Some("Error during unmarshalling from request json."))
        case e: Exception => throw e
      }.get
    }
  }
}
