package edge.jam.moonlight.core.api

import akka.actor.ActorSystem
import org.slf4j.Logger
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read
import edge.jam.moonlight.core.model._

import scala.concurrent.ExecutionContext
import scala.util.Try

class Api(
    logger: Logger,
    apiConfig: ApiConfig
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) {
  private implicit val formats = DefaultFormats

  import akka.http.scaladsl.server.Directives._

  val route =
    post {
      path("addLine") {
        entity(as[String]) { requestBody => addLine(requestBody) }
      }
    } ~
    get {
      path("status") { complete("OK") }
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

  def addLine(requestJson: String): Route = {
    val readLine = Try(read[Line](requestJson))

    readLine.map { line =>
      complete("Line added.")
    }.recover {
      case e: Exception => complete(s"Error during line unmarshaling from request json. Response message: ${e.getMessage}")
    }.getOrElse(complete("Unknown failure."))
  }
}

object Api {
  val module = new Module("moonlight-core")

  def main(args: Array[String]): Unit = {
    module.api.init()
  }
}
