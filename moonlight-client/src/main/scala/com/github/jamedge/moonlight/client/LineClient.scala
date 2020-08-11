package com.github.jamedge.moonlight.client

import com.github.jamedge.moonlight.core.model.Line
import com.github.jamedge.moonlight.core.service.line.LineService

import scala.concurrent.Future

class LineClient(
    lineService: LineService
) {
  def persist(line: Line): Future[Unit] = {
    lineService.addLine(line)
  }
}

