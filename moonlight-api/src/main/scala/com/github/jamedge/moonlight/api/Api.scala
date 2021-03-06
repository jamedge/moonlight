package com.github.jamedge.moonlight.api

import akka.actor.ActorSystem
import org.slf4j.Logger
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, RejectionHandler, RouteConcatenation}
import com.github.jamedge.moonlight.api.handlers.{ExceptionHandlerBuilder, RejectionHandlerBuilder}
import org.json4s.DefaultFormats
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.github.jamedge.moonlight.api.routes.{ApiLineRoutes, ApiLineageRoutes, ApiReportRoutes, ApiStatusRoutes}

import scala.concurrent.ExecutionContext

class Api(
    logger: Logger,
    apiConfig: ApiConfig,
    apiStatusRoutes: ApiStatusRoutes,
    apiLineRoutes: ApiLineRoutes,
    apiLineageRoutes: ApiLineageRoutes,
    apiReportRoutes: ApiReportRoutes
)(implicit val executionContext: ExecutionContext, actorSystem: ActorSystem)
  extends Directives with RouteConcatenation with DefaultFormats {

  implicit val rejectionHandler: RejectionHandler = RejectionHandlerBuilder.build()(logger)

  val route = cors() {
    handleExceptions(ExceptionHandlerBuilder.build()(logger)) {
      apiStatusRoutes.route ~
      apiLineRoutes.routes ~
      apiLineageRoutes.routes ~
      apiReportRoutes.routes
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
}

object Api {
  val module = new Module("moonlight-api")

  def main(args: Array[String]): Unit = {
    sys.addShutdownHook({
      module.terminate()
    })
    module.api.init()
  }
}
