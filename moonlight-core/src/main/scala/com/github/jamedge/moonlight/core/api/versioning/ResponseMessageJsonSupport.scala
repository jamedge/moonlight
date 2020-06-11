package com.github.jamedge.moonlight.core.api.versioning

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import com.github.jamedge.moonlight.core.api.routes.ResponseMessage
import org.json4s.jackson.Serialization.write

import scala.concurrent.ExecutionContext

case class ResponseMessageV1(message: String)

object ResponseMessageJsonSupport extends JsonSupport[ResponseMessage] {

  private def jsonEntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[ResponseMessage] = {
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaTypes.`application/json`), write(ResponseMessageV1(successResponse.message))))
    }
  }

  private def jsonV1EntityMarshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[ResponseMessage] = {
    Marshaller.withFixedContentType(MediaVersionTypes.`application/moonlight.v1+json`) { successResponse =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/moonlight.v1+json`), write(ResponseMessageV1(successResponse.message))))
    }
  }

  override implicit def marshaller(implicit ec: ExecutionContext): ToResponseMarshaller[ResponseMessage] = {
    Marshaller.oneOf(
      jsonEntityMarshaller,
      jsonV1EntityMarshaller
    )
  }
}
