package com.github.jamedge.moonlight.core.api

import com.github.jamedge.moonlight.core.Context
import com.softwaremill.macwire.wire
import com.github.jamedge.moonlight.core.service.{LineService, LineageService, OutputConfig}
import neotypes.{Driver, GraphDatabase}
import org.neo4j.driver.v1.AuthTokens
import pureconfig.ConfigSource
import shapeless._
import pureconfig.generic.auto._

import scala.concurrent.Future

class Module(app: String) extends Context(app) {
  lazy val dbConfig: DbConfig = ConfigSource.fromConfig(config).at("db").loadOrThrow[DbConfig]
  val neo4jDriver: Id[Driver[Future]] = GraphDatabase.
    driver[Future](dbConfig.neo4j.uri,
      AuthTokens.basic(dbConfig.neo4j.username, dbConfig.neo4j.password))

  lazy val apiConfig: ApiConfig = ConfigSource.fromConfig(config).at("api").loadOrThrow[ApiConfig]
  lazy val api: Api = wire[Api]

  lazy val lineService: LineService = wire[LineService]
  lazy val lineageService: LineageService = wire[LineageService]
  lazy val outputConfig: OutputConfig.Output = ConfigSource.fromConfig(config).at("output").loadOrThrow[OutputConfig.Output]
}