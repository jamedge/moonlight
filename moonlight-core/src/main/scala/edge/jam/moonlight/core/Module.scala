package edge.jam.moonlight.core

import com.typesafe.config.ConfigFactory

class Module {
  lazy val config = ConfigFactory.load().resolve().getConfig("moonlight-core")
}
