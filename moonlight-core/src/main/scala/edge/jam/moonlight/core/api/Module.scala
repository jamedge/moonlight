package edge.jam.moonlight.core.api

import akka.stream.ActorMaterializer
import edge.jam.moonlight.core.Context
import com.softwaremill.macwire.wire
import edge.jam.moonlight.core.model.LineService
import neotypes.{Driver, GraphDatabase}
import org.neo4j.driver.v1.AuthTokens
import pureconfig.loadConfigOrThrow
import shapeless._

import scala.concurrent.Future

class Module(app: String) extends Context(app) {
  implicit val materializer = ActorMaterializer()

  lazy val dbConfig: DbConfig = loadConfigOrThrow[DbConfig](config, "db")
  val neo4jDriver: Id[Driver[Future]] = GraphDatabase.
    driver[Future](dbConfig.neo4j.uri,
      AuthTokens.basic(dbConfig.neo4j.username, dbConfig.neo4j.password))

  lazy val apiConfig: ApiConfig = loadConfigOrThrow[ApiConfig](config, "api")
  lazy val api: Api = wire[Api]

  lazy val lineService: LineService = wire[LineService]
}
