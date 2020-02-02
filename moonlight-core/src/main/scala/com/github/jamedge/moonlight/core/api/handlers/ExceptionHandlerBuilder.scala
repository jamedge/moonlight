package com.github.jamedge.moonlight.core.api.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.ExceptionHandler
import org.slf4j.Logger

object ExceptionHandlerBuilder {
  def build()(implicit logger: Logger) = ExceptionHandler {
    case e: ApiException =>
      ErrorResponseBuilder.logAndGenerateResponse(
        e.responseStatusCode,
        e.responseMessage(),
        logger.error,
        Some(buildLogMessage(e))
      )
    case e: Exception =>
      ErrorResponseBuilder.logAndGenerateResponse(
        StatusCodes.InternalServerError,
        logger.error,
        Some(buildLogMessage(e))
      )
  }

  def buildLogMessage(e: Throwable): String = {
    s"\nSource message: ${e.getMessage}\nSource stacktrace:${e.getStackTrace.mkString("\n", "\n\t", "\n")}"
  }
}
