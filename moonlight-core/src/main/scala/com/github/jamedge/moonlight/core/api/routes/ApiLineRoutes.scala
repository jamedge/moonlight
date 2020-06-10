package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpCharsets, HttpEntity, HttpResponse, MediaType, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.github.jamedge.moonlight.core.model.{Line, LineV1, LineV2}
import com.github.jamedge.moonlight.core.service.line.LineService
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{read, write}

import scala.concurrent.ExecutionContext

case class SuccessResponse(message: String, smile: String)
case class SuccessResponseV1(message: String)
case class SuccessResponseV2(message: String, smile: String)

object MediaVersionTypes {
  def customMediaType(subType: String) = MediaType.customWithFixedCharset("application", subType, HttpCharsets.`UTF-8`)

  val `application/moonlight.v1+json`: MediaType.WithFixedCharset = customMediaType("moonlight.v1+json")
  val `application/moonlight.v2+json`: MediaType.WithFixedCharset = customMediaType("moonlight.v2+json")
}

trait JsonSupport {

  implicit val formats = DefaultFormats

  implicit def successResponseMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[SuccessResponse] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaTypes.`application/json`), write(SuccessResponseV2(successResponse.message, successResponse.smile))))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v1+json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/moonlight.v1+json`), write(SuccessResponseV1(successResponse.message))))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v2+json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/moonlight.v2+json`), write(SuccessResponseV2(successResponse.message, successResponse.smile))))
    }
  )

  def lineJsonEntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaTypes.`application/json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = read[Line](input)
      Line(tmp.name, tmp.owner, tmp.purpose, tmp.notes, tmp.details, tmp.io, tmp.processedBy, tmp.metrics, tmp.alerts, tmp.code)
    }

  def lineJsonV1EntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaVersionTypes.`application/moonlight.v1+json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = read[LineV1](input)
      Line(tmp.name, tmp.owner, tmp.purpose, tmp.notes, tmp.details, tmp.io, tmp.processedBy, tmp.metrics, tmp.alerts, tmp.code)
    }

  def lineJsonV2EntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaVersionTypes.`application/moonlight.v2+json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = read[LineV2](input)
      Line(tmp.name, tmp.owner, tmp.purpose, tmp.notes, tmp.details, tmp.io, tmp.processedBy, tmp.metrics, tmp.alerts, tmp.code)
    }

  implicit def lineUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.firstOf[HttpEntity, Line](
      lineJsonEntityUnmarshaller, lineJsonV1EntityUnmarshaller, lineJsonV2EntityUnmarshaller
    )
}

class ApiLineRoutes(
    lineService: LineService
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem) extends Directives with JsonSupport {

  def routes: Route = {
    addLine
  }

  private[routes] def addLine: Route = {
    path("line") {
      post {
        entity(as[Line]) { line =>
          complete(lineService.addLine(line).map(_ => SuccessResponse("Line added/updated.", "Of course :)")))
        }
      } ~
      get {
        complete(SuccessResponse("Test getting of line successful!", "yes!"))
      }
    }
  }
}
