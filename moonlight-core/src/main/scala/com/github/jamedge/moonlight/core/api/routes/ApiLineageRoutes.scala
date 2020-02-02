package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import com.github.jamedge.moonlight.core.service.lineage.{LineageGraphFormattedOutputType, LineageService}
import org.json4s.DefaultFormats

import scala.concurrent.ExecutionContext

class ApiLineageRoutes(
    lineageService: LineageService
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem) extends Directives {

  implicit val formats = DefaultFormats

  def routes: Route = {
    get {
      path("lineage" / "graph") {
        parameters("root_io") { rootIOElementName =>
          complete {
            lineageService.getLineageGraphJson(rootIOElementName).map { graphJson =>
              HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, graphJson))
            }
          }
        }
      } ~
      path("lineage" / "graph" / "md") { // TODO: distinguish this vs json output based on content type
        parameters("root_io") { rootIOElementName =>
          complete {
            lineageService.getLineageGraphFormatted(rootIOElementName, LineageGraphFormattedOutputType.Md).map { graphMd =>
              HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, graphMd))
            }
          }
        }
      } ~
      path("lineage" / "graph" / "json") {
        parameters("root_io") { rootIOElementName =>
          complete {
            lineageService.getLineageGraphFormatted(rootIOElementName, LineageGraphFormattedOutputType.Json).map { graphJson =>
              HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, graphJson))
            }
          }
        }
      }
    }
  }
}
