package edge.jam.moonlight.core.api

import akka.actor.ActorSystem
import org.slf4j.Logger
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.json4s.DefaultFormats

import scala.concurrent.ExecutionContext

class Api(
    logger: Logger,
    apiConfig: ApiConfig
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) {
  private implicit val formats = DefaultFormats

  import akka.http.scaladsl.server.Directives._

  val route =
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
}

object Api {
  val module = new Module("moonlight-core")

  def main(args: Array[String]): Unit = {
    module.api.init()
  }
}
