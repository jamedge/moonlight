package com.github.jamedge.moonlight.core.api.versioning.line

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.core.api.versioning.{HTMLGenerator, HTMLSupport, MediaVersionTypes}
import com.github.jamedge.moonlight.core.model.Line

import scala.concurrent.ExecutionContext

object LineHTMLSupport extends HTMLSupport[Line] {
  override implicit def marshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.oneOf(
      htmlEntityMarshaller,
      htmlV1EntityMarshaller
    )
  }

  private def htmlEntityMarshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.withOpenCharset(MediaTypes.`text/html`) { case (line, charset) =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaTypes.`text/html`, charset),
        generateLineHTML(LineV1.toLineV1(line))))
    }
  }

  private def htmlV1EntityMarshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[Line] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+html`) { line =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaVersionTypes.`text/moonlight.v1+html`),
        generateLineHTML(LineV1.toLineV1(line))))
    }
  }

  implicit def marshallerLines(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[List[Line]] = {
    Marshaller.oneOf(
      htmlEntityMarshallerLines,
      htmlV1EntityMarshallerLines
    )
  }

  private def htmlEntityMarshallerLines(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[List[Line]] = {
    Marshaller.withOpenCharset(MediaTypes.`text/html`) { case (lines, charset) =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaTypes.`text/html`, charset),
        lines.map(line => generateLineHTML(LineV1.toLineV1(line))).mkString("<br>")))
    }
  }

  private def htmlV1EntityMarshallerLines(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[List[Line]] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`text/moonlight.v1+html`) { lines =>
      HttpResponse(entity = HttpEntity(
        ContentType(MediaVersionTypes.`text/moonlight.v1+html`),
        lines.map(line => generateLineHTML(LineV1.toLineV1(line))).mkString("<br>")))
    }
  }

  /**
   * Generates HTML representation of the provided line.
   * @param line Provided line.
   * @param executionContext Execution context.
   * @param lineMDGenerator Line markdown generator.
   * @param lineHTMLGenerator Line HTML generator.
   * @return Generated html representation of the line.
   */
  def generateLineHTML(line: LineV1)(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator): String = {
    val result = for {
      lineMD <- lineMDGenerator.generateMd(line)
      lineHTML <- lineHTMLGenerator.generateHTML(lineMD)
    } yield lineHTML
    result.getOrElse(throw new LineHTMLGenerationException("Error generating line HTML!"))
  }
}

case class LineHTMLGenerationException(message: String) extends Exception(message)
