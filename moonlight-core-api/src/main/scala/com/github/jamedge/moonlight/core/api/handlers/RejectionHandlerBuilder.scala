package com.github.jamedge.moonlight.core.api.handlers

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import akka.http.scaladsl.model.StatusCodes.{Forbidden, MethodNotAllowed, NotFound, UnprocessableEntity}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, MethodRejection, MissingQueryParamRejection, RejectionHandler}
import org.slf4j.Logger

object RejectionHandlerBuilder {
  def build()(implicit logger: Logger): RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case AuthorizationFailedRejection =>
          ErrorResponseBuilder.logAndGenerateResponse(Forbidden, logger.warn, None)
        case MissingQueryParamRejection(parameterName) =>
          ErrorResponseBuilder.logAndGenerateResponse(
            UnprocessableEntity,
            s"Parameter $parameterName is missing!",
            logger.warn,
            None
          )
      }
      .handleAll[MethodRejection] { methodRejections =>
        val names = methodRejections.map(_.supported.name)
        ErrorResponseBuilder.logAndGenerateResponse(
          MethodNotAllowed,
          s"Method not allowed! Supported: ${names mkString " or "}!",
          logger.warn,
          None
        )
      }
      .handleNotFound {
        ErrorResponseBuilder.logAndGenerateResponse(NotFound, logger.warn, None)
      }
      .result()
      .withFallback(defaultRejectionResponse())

  private def defaultRejectionResponse(): RejectionHandler = {
    RejectionHandler.default.mapRejectionResponse {
      case response @ HttpResponse(_, _, ent: HttpEntity.Strict, _) =>
        val message = ent.data.utf8String.replaceAll("\"", """\"""")
        val errorResponse = ErrorResponse(
          response.status.intValue().toString,
          extractResponseUrl(response),
          ZonedDateTime
            .now(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
          ErrorResponseBuilder.extractTraceId(response),
          ErrorResponseDetails("BAD_REQUEST", message)
        )
        val errorResponseJson = ErrorResponseBuilder.buildErrorResponseJson(errorResponse)
        response.copy(entity = HttpEntity(ContentTypes.`application/json`, errorResponseJson))

      // pass through all other types of responses
      case other => other
    }
  }

  private def extractResponseUrl(response: HttpResponse): String = {
    val locationHeader = response.getHeader("Location")
    if (locationHeader.isPresent) locationHeader.get().value() else "Cannot be extracted."
  }
}
