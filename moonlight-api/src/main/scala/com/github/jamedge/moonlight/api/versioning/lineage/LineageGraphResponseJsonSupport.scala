package com.github.jamedge.moonlight.api.versioning.lineage

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.api.routes.LineageGraphResponse
import com.github.jamedge.moonlight.api.versioning.{JsonSupport, MediaVersionTypes}

import scala.concurrent.ExecutionContext

object LineageGraphResponseJsonSupport extends JsonSupport[LineageGraphResponse] {

  private def jsonEntityMarshaller(
      implicit executionContext: ExecutionContext,
      graphFormatter: GraphFormatter
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { lineageGraphResponse =>
      HttpResponse(entity = HttpEntity(
          ContentType(MediaTypes.`application/json`),
          graphFormatter.
            formatLineageGraph(
              lineageGraphResponse.graph,
              lineageGraphResponse.rootNode,
              LineageGraphFormattedOutputType.Json)))
    }
  }

  private def jsonV1EntityMarshaller(
      implicit executionContext: ExecutionContext,
      graphFormatter: GraphFormatter
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v1+json`) { lineageGraphResponse =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaVersionTypes.`application/moonlight.v1+json`),
        graphFormatter.
          formatLineageGraph(
            lineageGraphResponse.graph,
            lineageGraphResponse.rootNode,
            LineageGraphFormattedOutputType.Json)))
    }
  }

  implicit def marshaller(
      implicit ec: ExecutionContext,
      graphFormatter: GraphFormatter
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.oneOf(
      jsonEntityMarshaller,
      jsonV1EntityMarshaller
    )
  }
}
