package com.github.jamedge.moonlight.core.api.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpCharsets, HttpEntity, HttpResponse, MediaType, MediaTypes}
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.fasterxml.jackson.core.JsonParseException
import com.github.jamedge.moonlight.core.api.handlers.ApiException
import com.github.jamedge.moonlight.core.model.{Alert, AlertsFramework, Code, IO, IOElement, Line, LineV1, LineV2, Metric, MetricsFramework, Process, ProcessingFramework, ProcessingHistory, ProcessingHistoryRecord, Storage}
import com.github.jamedge.moonlight.core.service.line.LineService
import org.json4s.{DefaultFormats, MappingException}
import org.json4s.jackson.Serialization.read
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.ExecutionContext
import scala.util.Try

case class SuccessResponse(message: String, smile: String)
case class SuccessResponseV1(message: String)
case class SuccessResponseV2(message: String, smile: String)

object MediaVersionTypes {
  def customMediatype(subType: String) = MediaType.customWithFixedCharset("application", subType, HttpCharsets.`UTF-8`)

  val `application/moonlight.v1+json`: MediaType.WithFixedCharset = customMediatype("moonlight.v1+json")
  val `application/moonlight.v2+json`: MediaType.WithFixedCharset = customMediatype("moonlight.v2+json")
}

trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val successResponseJsonFormatV1: RootJsonFormat[SuccessResponseV1] = jsonFormat1(SuccessResponseV1)
  implicit val successResponseJsonFormatV2: RootJsonFormat[SuccessResponseV2] = jsonFormat2(SuccessResponseV2)

  implicit def successResponseMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[SuccessResponse] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaTypes.`application/json`), SuccessResponseV2(successResponse.message, successResponse.smile).toJson.compactPrint))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v1+json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/moonlight.v1+json`), SuccessResponseV1(successResponse.message).toJson.compactPrint))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v2+json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/moonlight.v2+json`), SuccessResponseV2(successResponse.message, successResponse.smile).toJson.compactPrint))
    }
  )

  implicit val storageJsonFormat = jsonFormat6(Storage)
  implicit val processingFrameworkJsonFormat = jsonFormat6(ProcessingFramework)
  implicit val processingHistoryJsonFormat = jsonFormat5(ProcessingHistory)
  implicit val processingHistoryRecordJsonFormat = jsonFormat3(ProcessingHistoryRecord)
  implicit val processJsonFormat = jsonFormat8(Process)
  implicit val ioElementJsonFormat = jsonFormat7(IOElement)
  implicit val ioJsonFormat = jsonFormat2(IO)
  implicit val metricsFrameworkJsonFormat = jsonFormat6(MetricsFramework)
  implicit val metricJsonFormat = jsonFormat7(Metric)
  implicit val alertsFrameworkJsonFormat = jsonFormat6(AlertsFramework)
  implicit val alertJsonFormat = jsonFormat7(Alert)
  implicit val codeJsonFormat = jsonFormat8(Code)
  implicit val lineJsonFormat = jsonFormat10(Line)
  implicit val lineV1JsonFormat = jsonFormat10(LineV1)
  implicit val lineV2JsonFormat = jsonFormat10(LineV2)

  def lineJsonEntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaTypes.`application/json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = input.parseJson.convertTo[Line]
      Line(tmp.name, tmp.owner, tmp.purpose, tmp.notes, tmp.details, tmp.io, tmp.processedBy, tmp.metrics, tmp.alerts, tmp.code)
    }

  def lineJsonV1EntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaVersionTypes.`application/moonlight.v1+json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = input.parseJson.convertTo[LineV1]
      Line(tmp.name, tmp.owner, tmp.purpose, tmp.notes, tmp.details, tmp.io, tmp.processedBy, tmp.metrics, tmp.alerts, tmp.code)
    }

  def lineJsonV2EntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaVersionTypes.`application/moonlight.v2+json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = input.parseJson.convertTo[LineV2]
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

  implicit val formats = DefaultFormats

  def routes: Route = {
    addLine
  }

  private[routes] def addLine: Route = {
    path("line") {
      post {
        entity(as[Line]) { line =>
          val result = for {
//            line <- Try(read[Line](requestBody))
            lineAdditionResult <- Try(lineService.addLine(line).map(_ =>
              SuccessResponse("Line added/updated.", "Of course :)")))
          } yield lineAdditionResult
          result.map(complete(_)).recover {
            case e@(_: MappingException | _: JsonParseException) =>
              throw new ApiException(e, BadRequest, Some("Error during unmarshalling from request json."))
            case e: Exception => throw e
          }.get
        }
      } ~
      get {
        complete(SuccessResponse("Test getting of line successful!", "yes!"))
      }
    }
  }
}
