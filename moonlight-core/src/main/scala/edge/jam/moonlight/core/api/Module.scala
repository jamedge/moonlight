package edge.jam.moonlight.core.api

import akka.stream.ActorMaterializer
import edge.jam.moonlight.core.Context

class Module(app: String) extends Context(app) {
  implicit val materializer = ActorMaterializer()
}
