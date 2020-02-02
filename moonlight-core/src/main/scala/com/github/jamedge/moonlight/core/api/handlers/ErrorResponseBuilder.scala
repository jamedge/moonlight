package com.github.jamedge.moonlight.core.api.handlers

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.extractRequest
import akka.http.scaladsl.server.Route

object ErrorResponse {
  import org.json4s.Formats
  def unmarshallErrorResponse(errorResponse: String)(implicit formats: Formats): ErrorResponse = {
    import org.json4s.jackson.Serialization.read
    read[ErrorResponse](errorResponse)
  }
}
case class ErrorResponse(
    status: String,
    path: String,
    time: String,
    traceId: String,
    errors: Iterable[ErrorResponseDetails]
)
case class ErrorResponseDetails(code: String = "INVALID_REQUEST_CODE", message: String)

object ErrorResponseBuilder {
  def buildErrorResponse(statusCode: StatusCode, errors: Iterable[ErrorResponseDetails]): Route = {
    import akka.http.scaladsl.server.Directives._
    extractRequest { request =>
      extractUri { uri =>
        val errorResponse = ErrorResponse(
          statusCode.intValue().toString,
          uri.path.toString() + uri.queryString().map(qs => s"?$qs").getOrElse(""),
          ZonedDateTime
            .now(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
          extractTraceId(request),
          errors
        )
        val errorResponseJson = buildErrorResponseJson(errorResponse)
        complete(
          HttpResponse(
            statusCode,
            entity = HttpEntity(ContentTypes.`application/json`, errorResponseJson)
          )
        )
      }
    }
  }

  def logAndGenerateResponse(
      statusCode: StatusCode,
      logMethod: String => Unit,
      extraLogMessage: Option[String]
  ): Route = {
    logAndGenerateResponse(statusCode, statusCode.defaultMessage(), logMethod, extraLogMessage)
  }

  def logAndGenerateResponse(
      statusCode: StatusCode,
      message: String,
      logMethod: String => Unit,
      extraLogMessage: Option[String]
  ): Route = {
    extractRequest { request =>
      val extraLogInfo = extraLogMessage.map(m => s"\nAdditional details: $m").getOrElse("")
      logMethod(
        s"Unable to process request. Response message: $message$extraLogInfo Request: ${request.toString()}"
      )
      ErrorResponseBuilder.buildErrorResponse(statusCode, message)
    }
  }

  def buildErrorResponse(statusCode: StatusCode): Route = {
    buildErrorResponse(statusCode, statusCode.defaultMessage())
  }

  def buildErrorResponse(statusCode: StatusCode, message: String): Route = {
    ErrorResponseBuilder.buildErrorResponse(
      statusCode,
      Seq(ErrorResponseDetails(statusCode.reason().replace(" ", "_").toUpperCase, message))
    )
  }

  def extractTraceId(httpMessage: HttpMessage): String = {
    val ddHeader = httpMessage.getHeader("trace_id")
    if (ddHeader.isPresent) ddHeader.get().value() else "0"
  }

  def buildErrorResponseJson(errorResponse: ErrorResponse): String = {
    import org.json4s.DefaultFormats
    import org.json4s.jackson.Serialization.write
    write[ErrorResponse](errorResponse)(DefaultFormats)
  }
}
