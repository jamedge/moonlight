package com.github.jamedge.moonlight.api.versioning.lineage

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.api.routes.LineageGraphResponse
import com.github.jamedge.moonlight.api.versioning.{FormattedOutputType, HTMLGenerator, JsonSupport, MediaVersionTypes}

import scala.concurrent.ExecutionContext

object LineageGraphResponseHTMLSupport extends JsonSupport[LineageGraphResponse] {

  private def htmlEntityMarshaller(
      implicit executionContext: ExecutionContext,
      graphFormatter: GraphFormatter,
      htmlGenerator: HTMLGenerator
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.withOpenCharset(MediaTypes.`text/html`) { case (lineageGraphResponse, charset) =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaTypes.`text/html`, charset),
        graphFormatter.
          formatLineageGraph(
            lineageGraphResponse.lineageGraph,
            FormattedOutputType.HTML)))
    }
  }

  private def htmlV1EntityMarshaller(
      implicit executionContext: ExecutionContext,
      graphFormatter: GraphFormatter,
      htmlGenerator: HTMLGenerator
  ): ToResponseMarshaller[LineageGraphResponse] = Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+html`) { lineageGraphResponse =>
    HttpResponse(entity = HttpEntity(
      ContentType(
        MediaVersionTypes.`text/moonlight.v1+html`),
        graphFormatter.
          formatLineageGraph(
            lineageGraphResponse.lineageGraph,
            FormattedOutputType.HTML)))
  }

  implicit def marshaller(
      implicit ec: ExecutionContext,
      graphFormatter: GraphFormatter,
      htmlGenerator: HTMLGenerator
  ): ToResponseMarshaller[LineageGraphResponse] = {
    Marshaller.oneOf(
      htmlEntityMarshaller,
      htmlV1EntityMarshaller
    )
  }
}

