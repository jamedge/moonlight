package edge.jam.moonlight.core.api

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.ExceptionHandler
import com.fasterxml.jackson.core.JsonParseException
import org.json4s.MappingException
import org.slf4j.Logger

object ExceptionHandlerBuilder {
  def build()(implicit logger: Logger) = ExceptionHandler {
    case e @ (_: MappingException | _: JsonParseException) =>
      logToConsole(e)
      complete(
        HttpResponse(
          StatusCodes.BadRequest,
          entity = s"Error during line unmarshalling from request json.\nResponse message:\n${e.getMessage}"))

    case e: Exception =>
      logToConsole(e)
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = "Internal server error."))
  }

  private def logToConsole(e: Throwable)(implicit logger: Logger): Unit = {
    logger.debug(e.getMessage)
    logger.debug(e.getStackTrace.mkString("\n\t"))
  }
}
