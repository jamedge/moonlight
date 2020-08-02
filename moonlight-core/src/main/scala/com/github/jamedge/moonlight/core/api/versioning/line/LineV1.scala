package com.github.jamedge.moonlight.core.api.versioning.line

import com.github.jamedge.moonlight.core.model.{Alert, Code, IO, Line, Metadata, Metric, Process}

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

object LineV1 {
  def toLineV1(line: Line): LineV1 = {
    LineV1(
      line.name,
      line.owner,
      line.purpose,
      line.notes,
      line.details,
      line.io,
      line.processedBy,
      line.metrics,
      line.alerts,
      line.code)
  }

  def fromLineV1(lineV1: LineV1): Line = {
    Line(
      lineV1.name,
      lineV1.owner,
      lineV1.purpose,
      lineV1.notes,
      lineV1.details,
      lineV1.io,
      lineV1.processedBy,
      lineV1.metrics,
      lineV1.alerts,
      lineV1.code)
  }

  def trim(line: Line): Line = {
    fromLineV1(toLineV1(line))
  }
}
