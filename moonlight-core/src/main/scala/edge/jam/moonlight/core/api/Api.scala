package edge.jam.moonlight.core.api

import akka.actor.ActorSystem
import org.slf4j.Logger
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read
import edge.jam.moonlight.core.model._

import scala.concurrent.{ExecutionContext, Future}

class Api(
    logger: Logger,
    apiConfig: ApiConfig,
    lineService: LineService
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) {
  private implicit val formats = DefaultFormats

  import akka.http.scaladsl.server.Directives._

  val route =
    handleExceptions(ExceptionHandlerBuilder.build()(logger)) {
      post {
        path("addLine") {
          entity(as[String]) { requestBody => addLine(requestBody) }
        }
      } ~
        get {
          path("status") {
            complete("OK")
          }
        } ~
        get {
          path("codeTest") {
            complete {
              getAllCodeMetadata().map(_.getOrElse("Error"))
            }
          }
        }
    }
  logger.info("Route initialized.")

  def init(): Unit = {
    logger.info("Initialising API")
    Http().bindAndHandle(
      route,
      apiConfig.server.host,
      apiConfig.server.port
    )
    logger.info("Server started.")
  }

  def getAllCodeMetadata(): Future[Option[String]] = {
    lineService.getAllCodeMetadata()
  }

  def addLine(requestJson: String): Route = {
    val line = read[Line](requestJson)
    // TODO: add saving to db
    complete("Line added.")
  }
}

object Api {
  val module = new Module("moonlight-core")

  def main(args: Array[String]): Unit = {
    module.api.init()
  }
}
