package edge.jam.moonlight.core.api

import akka.stream.ActorMaterializer
import edge.jam.moonlight.core.Context
import com.softwaremill.macwire.wire
import pureconfig.loadConfigOrThrow

class Module(app: String) extends Context(app) {
  implicit val materializer = ActorMaterializer()

  val api: Api = wire[Api]
  lazy val apiConfig: ApiConfig = loadConfigOrThrow[ApiConfig](config, "api")
}
