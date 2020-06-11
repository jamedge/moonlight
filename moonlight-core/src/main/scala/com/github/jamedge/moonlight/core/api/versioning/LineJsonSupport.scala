package com.github.jamedge.moonlight.core.api.versioning

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.github.jamedge.moonlight.core.model.{Alert, Code, IO, Line, Metadata, Metric, Process}
import org.json4s.jackson.Serialization.read

case class LineV1(
    name: String,
    owner: Option[String],
    purpose: Option[String],
    notes: Option[List[String]],
    details: Option[Map[String, String]],
    io: List[IO],
    processedBy: List[Process],
    metrics: List[Metric],
    alerts: List[Alert],
    code: Option[Code]
) extends Metadata

object LineJsonSupport extends JsonSupport[Line] {

  private def jsonEntityUnmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.
      forContentTypes(MediaTypes.`application/json`).
      mapWithCharset { (data, charset) =>
        val tmp = read[Line](decode(data, charset))
        Line(
          tmp.name,
          tmp.owner,
          tmp.purpose,
          tmp.notes,
          tmp.details,
          tmp.io,
          tmp.processedBy,
          tmp.metrics,
          tmp.alerts,
          tmp.code)
    }

  private def jsonV1EntityUnmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.byteStringUnmarshaller.
      forContentTypes(MediaVersionTypes.`application/moonlight.v1+json`).
      mapWithCharset { (data, charset) =>
        val tmp = read[LineV1](decode(data, charset))
        Line(
          tmp.name,
          tmp.owner,
          tmp.purpose,
          tmp.notes,
          tmp.details,
          tmp.io,
          tmp.processedBy,
          tmp.metrics,
          tmp.alerts,
          tmp.code)
      }

  override implicit def unmarshaller(implicit matherializer: Materializer): FromEntityUnmarshaller[Line] =
    Unmarshaller.firstOf[HttpEntity, Line](
      jsonEntityUnmarshaller,
      jsonV1EntityUnmarshaller
    )

  //TODO: add line marshaller
}
