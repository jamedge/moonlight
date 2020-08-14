package com.github.jamedge.moonlight.client

import com.github.jamedge.moonlight.core.service.line.{LinePersistenceLayer, LineService}
import com.github.jamedge.moonlight.core.service.lineage.{LineagePersistenceLayer, LineageService}
import com.github.jamedge.moonlight.core.{Context, DbConfig}
import com.softwaremill.macwire.wire
import neotypes.{Driver, GraphDatabase}
import org.neo4j.driver.v1.AuthTokens
import pureconfig.ConfigSource
import shapeless.Id
import pureconfig.generic.auto._

import scala.concurrent.Future

class Module(app: String) extends Context(app) {
  lazy val dbConfig: DbConfig = ConfigSource.fromConfig(config).at("db").loadOrThrow[DbConfig]
  lazy val neo4jDriver: Id[Driver[Future]] = GraphDatabase.
    driver[Future](dbConfig.neo4j.uri,
    AuthTokens.basic(dbConfig.neo4j.username, dbConfig.neo4j.password))

  lazy val lineService: LineService = wire[LineService]
  lazy val linePersistenceLayer: LinePersistenceLayer = wire[LinePersistenceLayer]

  lazy val lineageService: LineageService = wire[LineageService]
  lazy val lineagePersistenceLayer: LineagePersistenceLayer = wire[LineagePersistenceLayer]

  lazy val lineClient: LineClient = wire[LineClient]
}
