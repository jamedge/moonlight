package com.github.jamedge.moonlight.core.api.versioning.line

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.core.api.versioning.{HTMLSupport, MDSupport, MediaVersionTypes}
import com.github.jamedge.moonlight.core.model.Line

import scala.concurrent.ExecutionContext

object LineMDSupport extends MDSupport[Line] {
  override implicit def marshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.oneOf(
      mdEntityMarshaller,
      mdV1EntityMarshaller
    )
  }

  private def mdEntityMarshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.withOpenCharset(MediaTypes.`text/markdown`) { case (line, charset) =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaTypes.`text/markdown`, charset),
        lineMDGenerator.generateMd(line).getOrElse(throw new LineMDGenerationException("Error generating line MD!"))))
    }
  }

  private def mdV1EntityMarshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+markdown`) { line =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaVersionTypes.`text/moonlight.v1+markdown`),
        lineMDGenerator.
          generateMdV1(LineV1.toLineV1(line)).
          getOrElse(throw new LineMDGenerationException("Error generating line MD!"))))
    }
  }
}

case class LineMDGenerationException(message: String) extends Exception(message)
