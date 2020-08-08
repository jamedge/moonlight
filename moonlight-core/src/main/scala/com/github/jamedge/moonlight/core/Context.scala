package com.github.jamedge.moonlight.core

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class Context(app: String) {
  lazy val config = ConfigFactory.load().resolve().getConfig(app)
  lazy val logger = LoggerFactory.getLogger(getClass)
  implicit lazy val executionContext = ExecutionContext.global
}
