package com.github.jamedge.moonlight.core.api.versioning.line

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.core.api.versioning.{HTMLSupport, MediaVersionTypes}
import com.github.jamedge.moonlight.core.model.Line

import scala.concurrent.ExecutionContext

object LineHTMLSupport extends HTMLSupport[Line] {
  override implicit def marshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[Line] = {
    Marshaller.oneOf(
      htmlEntityMarshaller,
      htmlV1EntityMarshaller
    )
  }

  private def htmlEntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[Line] = {
    Marshaller.withOpenCharset(MediaTypes.`text/html`) { case (line, charset) =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaTypes.`text/html`, charset),
        line.toString)) //TODO: produce html here.. Try to generate Markdown first and then convert to html
    }
  }

  private def htmlV1EntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[Line] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+html`) { line =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaVersionTypes.`text/moonlight.v1+html`),
        line.toString)) //TODO: produce html here.. Try to generate Markdown first and then convert to html
    }
  }
}
