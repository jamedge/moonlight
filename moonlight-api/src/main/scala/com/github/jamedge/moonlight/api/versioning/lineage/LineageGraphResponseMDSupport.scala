package com.github.jamedge.moonlight.api.versioning.lineage

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.api.routes.LineageGraphResponse
import com.github.jamedge.moonlight.api.versioning.{FormattedOutputType, HTMLGenerator, MediaVersionTypes}

import scala.concurrent.ExecutionContext

object LineageGraphResponseMDSupport {

  private def mdEntityMarshaller(
      implicit executionContext: ExecutionContext,
      htmlGenerator: HTMLGenerator,
      graphFormatter: GraphFormatter
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.withOpenCharset(MediaTypes.`text/markdown`) { case (lineageGraphResponse, charset) =>
      HttpResponse(entity = HttpEntity(
          ContentType(MediaTypes.`text/markdown`, charset),
          graphFormatter. // TODO: update graph formatter to use the whole lineage graph object
            formatLineageGraph(
              lineageGraphResponse.graph,
              lineageGraphResponse.rootNode,
              FormattedOutputType.Md)))
    }
  }

  private def mdV1EntityMarshaller(
      implicit executionContext: ExecutionContext,
      graphFormatter: GraphFormatter
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+markdown`) { lineageGraphResponse =>
      HttpResponse(entity = HttpEntity(
        ContentType(
          MediaVersionTypes.`text/moonlight.v1+markdown`),
        graphFormatter.
          formatLineageGraph(
            lineageGraphResponse.graph,
            lineageGraphResponse.rootNode,
            FormattedOutputType.Md)))
    }
  }

  implicit def marshaller(
      implicit ec: ExecutionContext,
      graphFormatter: GraphFormatter,
      htmlGenerator: HTMLGenerator
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.oneOf(
      mdEntityMarshaller,
      mdV1EntityMarshaller
    )
  }
}
