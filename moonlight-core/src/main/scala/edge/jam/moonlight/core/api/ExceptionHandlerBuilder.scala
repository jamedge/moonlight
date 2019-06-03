package edge.jam.moonlight.core.api

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.ExceptionHandler
import com.fasterxml.jackson.core.JsonParseException
import org.json4s.MappingException
import org.slf4j.Logger

object ExceptionHandlerBuilder {
  def build()(implicit logger: Logger) = ExceptionHandler {
    case e: MappingException =>
      logToConsole(e)
      complete(
        HttpResponse(
          StatusCodes.BadRequest,
          entity = s"Error during line unmarshaling from request json. Response message: ${e.getMessage}"))

    case e: JsonParseException =>
      logToConsole(e)
      complete(
        HttpResponse(
          StatusCodes.BadRequest,
          entity = s"Error during line unmarshaling from request json. Response message: ${e.getMessage}"))

    case e: Exception =>
      logToConsole(e)
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = "Internal server error."))
  }

  private def logToConsole(e: Exception)(implicit logger: Logger): Unit = {
    logger.info(e.getMessage)
    logger.info(e.getStackTrace.mkString("\n\t"))
  }
}
