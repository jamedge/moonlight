package com.github.jamedge.moonlight.core.api.versioning

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpCharset, HttpCharsets, HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import akka.util.ByteString
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{read, write}

import scala.concurrent.ExecutionContext

trait JsonSupport[T <: Object] {
  implicit val formats = DefaultFormats

  /**
  * Base marshaller for the object of class <T>. Override it before usage.
   * @param executionContext Implicit execution context.
   * @return Resulting marshaller.
   */
  implicit def marshaller(implicit executionContext: ExecutionContext): ToResponseMarshaller[T] = {
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { requestObject =>
      HttpResponse(
        entity = HttpEntity(ContentType(MediaTypes.`application/json`), write(requestObject)))
    }
  }

  /**
  * Base unmarshaller for the object of class <T>. Override it before usage.
   * @param matherializer Implicit matherializer.
   * @return Resulting unmarshaller.
   */
  implicit def unmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[T] = {
    Unmarshaller.
      byteStringUnmarshaller.
      forContentTypes(MediaTypes.`application/json`).
      mapWithCharset((data, charset) => read(decode(data, charset)))
  }

  /**
  * Decodes the input data.
   * @param data Input data.
   * @param charset Input charset.
   * @return Decoded string.
   */
  protected def decode(data: ByteString, charset: HttpCharset): String = {
    if (charset == HttpCharsets.`UTF-8`) {
      data.utf8String
    } else {
      data.decodeString(charset.nioCharset.name)
    }
  }
}
