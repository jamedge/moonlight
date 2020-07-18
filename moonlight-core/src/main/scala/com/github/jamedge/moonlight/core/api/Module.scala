package com.github.jamedge.moonlight.core.api

import com.github.jamedge.moonlight.core.Context
import com.github.jamedge.moonlight.core.api.routes.{ApiLineRoutes, ApiLineageRoutes, ApiStatusRoutes}
import com.github.jamedge.moonlight.core.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.core.api.versioning.line.LineMDGenerator
import com.github.jamedge.moonlight.core.api.versioning.lineage.{GraphFormatter, OutputConfig}
import com.github.jamedge.moonlight.core.service.line.{LinePersistenceLayer, LineService}
import com.softwaremill.macwire.wire
import com.github.jamedge.moonlight.core.service.lineage.{LineagePersistenceLayer, LineageService}
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

  lazy val apiStatusRoutes: ApiStatusRoutes = wire[ApiStatusRoutes]
  lazy val apiLineRoutes: ApiLineRoutes = wire[ApiLineRoutes]
  lazy val apiLineageRoutes: ApiLineageRoutes = wire[ApiLineageRoutes]

  lazy val lineService: LineService = wire[LineService]
  lazy val linePersistenceLayer: LinePersistenceLayer = wire[LinePersistenceLayer]

  lazy val lineageService: LineageService = wire[LineageService]
  lazy val lineagePersistenceLayer: LineagePersistenceLayer = wire[LineagePersistenceLayer]
  lazy val lineageOutputConfig: OutputConfig.Output = ConfigSource.fromConfig(config).at("output").loadOrThrow[OutputConfig.Output]

  implicit lazy val lineMDGenerator: LineMDGenerator = wire[LineMDGenerator]
  implicit lazy val htmlGenerator: HTMLGenerator = wire[HTMLGenerator]

  implicit lazy val lineageGraphFormatter: GraphFormatter = wire[GraphFormatter]
}
