package com.github.jamedge.moonlight.core.api

import akka.actor.ActorSystem
import com.github.jamedge.moonlight.core.Context

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ApiContext(app: String) extends Context(app) {
  private var actorSystemInitialized = false

  implicit lazy val actorSystem: ActorSystem = {
    val actorSystem = ActorSystem(s"$app-actor-system", config)
    actorSystemInitialized = true
    logger.info(s"$app ActorSystem was initialised")
    actorSystem
  }

  def terminate(): Unit = {
    logger.info("Terminating application")
    if (actorSystemInitialized) {
      logger.info("Stopping actor system")
      val whenTerminated = actorSystem.terminate()
      Await.result(whenTerminated, Duration.Inf)
    }
  }
}
