package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.{Directives, Route}
import com.fasterxml.jackson.core.JsonParseException
import com.github.jamedge.moonlight.core.api.{ApiVersion, VersionRouteMapping}
import com.github.jamedge.moonlight.core.api.handlers.ApiException
import com.github.jamedge.moonlight.core.model.Line
import com.github.jamedge.moonlight.core.service.line.LineService
import org.json4s.{DefaultFormats, MappingException}
import org.json4s.jackson.Serialization.{read, write}
import com.github.jamedge.moonlight.core.api.ApiVersion.VersionMappingOps

import scala.concurrent.ExecutionContext
import scala.util.Try

case class SuccessResponse(message: String)

class ApiLineRoutes(
    lineService: LineService
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem) extends Directives {

  implicit val formats = DefaultFormats

  def routes: Route = {
    addLine
  }

  private def addLine: Route = {
    extractRequest { request =>
      request.matchApiVersion(
        Seq(VersionRouteMapping(ApiVersion.V1, addLineImplementationV1)) // Won't do versioning until stable
      )
    }
  }

  private[routes] def addLineImplementationV1: Route = {
    path("line") {
      post {
        entity(as[String]) { requestBody =>
          val result = for {
            line <- Try(read[Line](requestBody))
            lineAdditionResult <- Try(lineService.addLine(line).map(_ =>
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(ContentTypes.`application/json`, write(SuccessResponse("Line added/updated."))))))
          } yield lineAdditionResult
          result.map(complete(_)).recover {
            case e@(_: MappingException | _: JsonParseException) =>
              throw new ApiException(e, BadRequest, Some("Error during unmarshalling from request json."))
            case e: Exception => throw e
          }.get
        }
      } ~
      get {
        complete(
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(ContentTypes.`application/json`, write(SuccessResponse("Test getting of line successful!")))))
      }
    }
  }
}
