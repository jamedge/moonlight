package com.github.jamedge.moonlight.api.versioning.responsemessage

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.api.routes.ResponseMessage
import com.github.jamedge.moonlight.api.versioning.{JsonSupport, MediaVersionTypes}
import org.json4s.jackson.Serialization.write

import scala.concurrent.ExecutionContext

object ResponseMessageJsonSupport extends JsonSupport[ResponseMessage] {

  private def jsonEntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[ResponseMessage] = {
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(
          ContentType(
            MediaTypes.`application/json`),
            write(ResponseMessageV1.toResponseMessageV1(successResponse))))
    }
  }

  private def jsonV1EntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[ResponseMessage] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v1+json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(
          ContentType(
            MediaVersionTypes.`application/moonlight.v1+json`),
            write(ResponseMessageV1.toResponseMessageV1(successResponse))))
    }
  }

  override implicit def marshaller(implicit ec: ExecutionContext): ToResponseMarshaller[ResponseMessage] = {
    Marshaller.oneOf(
      jsonEntityMarshaller,
      jsonV1EntityMarshaller
    )
  }
}
