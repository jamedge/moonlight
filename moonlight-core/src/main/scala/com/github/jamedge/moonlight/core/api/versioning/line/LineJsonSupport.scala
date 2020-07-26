package com.github.jamedge.moonlight.core.api.versioning.line

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.github.jamedge.moonlight.core.api.versioning.{JsonSupport, MediaVersionTypes}
import com.github.jamedge.moonlight.core.model.Line
import org.json4s.jackson.Serialization.{read, write}

import scala.concurrent.ExecutionContext

object LineJsonSupport extends JsonSupport[Line] {

  private def jsonEntityUnmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.
      forContentTypes(MediaTypes.`application/json`).
      mapWithCharset { (data, charset) =>
        val tmp = read[LineV1](decode(data, charset))
        LineV1.fromLineV1(tmp)
    }

  private def jsonV1EntityUnmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.
      forContentTypes(MediaVersionTypes.`application/moonlight.v1+json`).
      mapWithCharset { (data, charset) =>
        val tmp = read[LineV1](decode(data, charset))
        LineV1.fromLineV1(tmp)
      }

  override implicit def unmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.firstOf[HttpEntity, Line](
      jsonEntityUnmarshaller,
      jsonV1EntityUnmarshaller
    )

  private def jsonEntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[Line] = {
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { line =>
      HttpResponse(entity = HttpEntity(
          ContentType(
            MediaTypes.`application/json`),
            write(LineV1.toLineV1(line))))
    }
  }

  private def jsonV1EntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[Line] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v1+json`) { line =>
      HttpResponse(entity = HttpEntity(
        ContentType(
          MediaVersionTypes.`application/moonlight.v1+json`),
          write(LineV1.toLineV1(line))))
    }
  }

  override implicit def marshaller(implicit ec: ExecutionContext): ToResponseMarshaller[Line] = {
    Marshaller.oneOf(
      jsonEntityMarshaller,
      jsonV1EntityMarshaller
    )
  }

  private def jsonEntityMarshallerLines(implicit executionContext: ExecutionContext): ToResponseMarshaller[List[Line]] = {
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { lines =>
      HttpResponse(entity = HttpEntity(
        ContentType(
          MediaTypes.`application/json`),
        write(lines.map(LineV1.toLineV1))))
    }
  }

  private def jsonV1EntityMarshallerLines(implicit executionContext: ExecutionContext): ToResponseMarshaller[List[Line]] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v1+json`) { lines =>
      HttpResponse(entity = HttpEntity(
        ContentType(
          MediaVersionTypes.`application/moonlight.v1+json`),
        write(lines.map(LineV1.toLineV1))))
    }
  }

  implicit def marshallerLines(implicit ec: ExecutionContext): ToResponseMarshaller[List[Line]] = {
    Marshaller.oneOf(
      jsonEntityMarshallerLines,
      jsonV1EntityMarshallerLines
    )
  }
}
