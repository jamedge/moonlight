package com.github.jamedge.moonlight.api.versioning.line

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.api.versioning.{MDSupport, MediaVersionTypes}
import com.github.jamedge.moonlight.api.versioning.{HTMLSupport, MDSupport, MediaVersionTypes}
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
        lineMDGenerator.
          generateMd(line).
          getOrElse(throw LineMDGenerationException("Error generating line MD!"))))
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
          generateMd(LineV1.trim(line)).
          getOrElse(throw LineMDGenerationException("Error generating line MD!"))))
    }
  }

  private def mdEntityMarshallerLines(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator
  ): ToResponseMarshaller[List[Line]] = {
    Marshaller.withOpenCharset(MediaTypes.`text/markdown`) { case (lines, charset) =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaTypes.`text/markdown`, charset),
        lines.map(line => lineMDGenerator.
          generateMd(line).
          getOrElse(throw LineMDGenerationException("Error generating line MD!"))).mkString("<br>")))
    }
  }

  private def mdV1EntityMarshallerLines(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator
  ): ToResponseMarshaller[List[Line]] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+markdown`) { lines =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaVersionTypes.`text/moonlight.v1+markdown`),
        lines.map(line => lineMDGenerator.
          generateMd(LineV1.trim(line)).
          getOrElse(throw LineMDGenerationException("Error generating line MD!"))).mkString("\n\n")))
    }
  }

  implicit def marshallerLines(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator
  ): ToResponseMarshaller[List[Line]] = {
    Marshaller.oneOf(
      mdEntityMarshallerLines,
      mdV1EntityMarshallerLines
    )
  }
}

case class LineMDGenerationException(message: String) extends Exception(message)
