package com.github.jamedge.moonlight.core.api.routes

import akka.http.scaladsl.server.{Directives, Route}
import org.json4s.DefaultFormats

class ApiStatusRoutes extends Directives with DefaultFormats {
  def route: Route = {
    get {
      path("status") {
        complete("OK")
      }
    }
  }
}
