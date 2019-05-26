package edge.jam.moonlight.core

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class Context(app: String) {
  lazy val config = ConfigFactory.load().resolve().getConfig(app)
  lazy val logger = LoggerFactory.getLogger(getClass)
  implicit lazy val executionContext = ExecutionContext.global

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
