package com.github.jamedge.moonlight.core.api.versioning.line

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer
import com.github.jamedge.moonlight.core.api.routes.ResponseMessage
import com.github.jamedge.moonlight.core.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.core.api.versioning.responsemessage.ResponseMessageJsonSupport
import com.github.jamedge.moonlight.core.model.Line

import scala.concurrent.ExecutionContext

trait LineRoutesSupport {
  implicit def lineUnmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    LineJsonSupport.unmarshaller

  implicit def lineMarshaller(
      implicit ec: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      htmlGenerator: HTMLGenerator
  ): ToResponseMarshaller[Line] =
    Marshaller.oneOf(
      LineJsonSupport.marshaller,
      LineHTMLSupport.marshaller,
      LineMDSupport.marshaller
    )

  implicit def linesMarshaller(
      implicit ec: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      htmlGenerator: HTMLGenerator
  ): ToResponseMarshaller[List[Line]] =
    Marshaller.oneOf(
      LineJsonSupport.marshallerLines,
      LineMDSupport.marshallerLines,
      LineHTMLSupport.marshallerLines
    )

  implicit def ResponseMessageMarshaller(implicit ec: ExecutionContext): ToResponseMarshaller[ResponseMessage] =
    ResponseMessageJsonSupport.marshaller
}
