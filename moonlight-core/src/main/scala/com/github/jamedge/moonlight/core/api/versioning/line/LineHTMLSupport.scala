package com.github.jamedge.moonlight.core.api.versioning.line

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.core.api.versioning.{HTMLSupport, MediaVersionTypes}
import com.github.jamedge.moonlight.core.model.Line

import scala.concurrent.ExecutionContext

object LineHTMLSupport extends HTMLSupport[Line] {
  override implicit def marshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: LineHTMLGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.oneOf(
      htmlEntityMarshaller,
      htmlV1EntityMarshaller
    )
  }

  private def htmlEntityMarshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: LineHTMLGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.withOpenCharset(MediaTypes.`text/html`) { case (line, charset) =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaTypes.`text/html`, charset),
        generateLineHTML(line)))
    }
  }

  private def htmlV1EntityMarshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: LineHTMLGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+html`) { line =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaVersionTypes.`text/moonlight.v1+html`),
        generateLineV1HTML(LineV1.toLineV1(line))))
    }
  }

  private def generateLineHTML(line: Line)(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: LineHTMLGenerator): String = {
    val result = for {
      lineMD <- lineMDGenerator.generateMd(line)
      lineHTML <- lineHTMLGenerator.generateHTML(lineMD)
    } yield lineHTML
    result.getOrElse(throw new LineHTMLGenerationException("Error generating line HTML!"))
  }

  private def generateLineV1HTML(line: LineV1)(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: LineHTMLGenerator): String = {
    val result = for {
      lineMD <- lineMDGenerator.generateMdV1(line)
      lineHTML <- lineHTMLGenerator.generateHTML(lineMD)
    } yield lineHTML
    result.getOrElse(throw new LineHTMLGenerationException("Error generating line HTML!"))
  }
}

case class LineHTMLGenerationException(message: String) extends Exception(message)
