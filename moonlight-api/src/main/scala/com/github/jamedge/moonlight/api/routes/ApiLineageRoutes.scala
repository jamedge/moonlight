package com.github.jamedge.moonlight.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import com.github.jamedge.moonlight.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.api.versioning.lineage.{GraphFormatter, LineageGraphResponseHTMLSupport, LineageGraphResponseJsonSupport, LineageGraphResponseMDSupport}
import com.github.jamedge.moonlight.core.service.fixture.{GraphBuilder, LineageGraph, LineageService}
import org.json4s.DefaultFormats

import scala.concurrent.ExecutionContext

case class LineageGraphResponse(lineageGraph: LineageGraph)

trait LineageRoutesSupport {
  implicit def lineageGraphResponseMarshaller(
      implicit ec: ExecutionContext,
      graphFormatter: GraphFormatter,
      HTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[LineageGraphResponse] =
    Marshaller.oneOf(
      LineageGraphResponseJsonSupport.marshaller,
      LineageGraphResponseHTMLSupport.marshaller,
      LineageGraphResponseMDSupport.marshaller
    )
}

class ApiLineageRoutes(
    lineageService: LineageService
) (
    implicit val executionContext: ExecutionContext,
    actorSystem: ActorSystem,
    graphFormatter: GraphFormatter,
    HTMLGenerator: HTMLGenerator
)
  extends Directives with LineageRoutesSupport {

  implicit val formats = DefaultFormats

  def routes: Route = {
    path("lineage" / "graph" / Segment) { rootIOElementName =>
      get {
        complete(lineageService.getLineageGraph(rootIOElementName).map(LineageGraphResponse))
      }
    } ~
      path("lineage" / "graph_source" / Segment) { rootIOElementName =>
        get {
          import scalax.collection.io.json._
          complete {
            lineageService.getLineageGraph(rootIOElementName).map { graph =>
              HttpResponse(entity = HttpEntity(
                ContentType(MediaTypes.`application/json`),
                graph.graph.toJson(GraphBuilder.createLineageGraphJsonDescriptor())))
            }
          }
        }
      }
  }
}
