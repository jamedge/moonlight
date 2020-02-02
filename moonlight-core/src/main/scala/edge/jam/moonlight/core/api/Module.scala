package edge.jam.moonlight.core.api

import edge.jam.moonlight.core.Context
import com.softwaremill.macwire.wire
import edge.jam.moonlight.core.model.{LineService, OutputConfig}
import neotypes.{Driver, GraphDatabase}
import org.neo4j.driver.v1.AuthTokens
import pureconfig.{ConfigSource, loadConfigOrThrow}
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
  lazy val outputConfig: OutputConfig.Output = ConfigSource.fromConfig(config).at("output").loadOrThrow[OutputConfig.Output]
}
