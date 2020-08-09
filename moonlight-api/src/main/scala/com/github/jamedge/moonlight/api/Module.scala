package com.github.jamedge.moonlight.api

import com.github.jamedge.moonlight.api.versioning.lineage.OutputConfig.Output
import com.github.jamedge.moonlight.api.routes.{ApiLineRoutes, ApiLineageRoutes, ApiReportRoutes, ApiStatusRoutes}
import com.github.jamedge.moonlight.api.versioning.HTMLGenerator
import com.github.jamedge.moonlight.api.versioning.line.LineMDGenerator
import com.github.jamedge.moonlight.api.versioning.lineage.{GraphFormatter, OutputConfig}
import com.github.jamedge.moonlight.core.DbConfig
import com.github.jamedge.moonlight.core.service.line.{LinePersistenceLayer, LineService}
import com.softwaremill.macwire.wire
import com.github.jamedge.moonlight.core.service.lineage.{LineagePersistenceLayer, LineageService}
import neotypes.{Driver, GraphDatabase}
import org.neo4j.driver.v1.AuthTokens
import pureconfig.ConfigSource
import shapeless._
import pureconfig.generic.auto._

import scala.concurrent.Future

class Module(app: String) extends ApiContext(app) {
  lazy val dbConfig: DbConfig = ConfigSource.fromConfig(config).at("db").loadOrThrow[DbConfig]
  val neo4jDriver: Id[Driver[Future]] = GraphDatabase.
    driver[Future](dbConfig.neo4j.uri,
      AuthTokens.basic(dbConfig.neo4j.username, dbConfig.neo4j.password))

  lazy val apiConfig: ApiConfig = ConfigSource.fromConfig(config).at("api").loadOrThrow[ApiConfig]
  lazy val api: Api = wire[Api]

  lazy val apiStatusRoutes: ApiStatusRoutes = wire[ApiStatusRoutes]
  lazy val apiLineRoutes: ApiLineRoutes = wire[ApiLineRoutes]
  lazy val apiLineageRoutes: ApiLineageRoutes = wire[ApiLineageRoutes]
  lazy val apiReportRoutes: ApiReportRoutes = wire[ApiReportRoutes]

  lazy val lineService: LineService = wire[LineService]
  lazy val linePersistenceLayer: LinePersistenceLayer = wire[LinePersistenceLayer]

  lazy val lineageService: LineageService = wire[LineageService]
  lazy val lineagePersistenceLayer: LineagePersistenceLayer = wire[LineagePersistenceLayer]
  lazy val lineageOutputConfig: Output = ConfigSource.fromConfig(config).at("output").loadOrThrow[OutputConfig.Output]

  implicit lazy val lineMDGenerator: LineMDGenerator = wire[LineMDGenerator]
  implicit lazy val htmlGenerator: HTMLGenerator = wire[HTMLGenerator]

  implicit lazy val lineageGraphFormatter: GraphFormatter = wire[GraphFormatter]
}
