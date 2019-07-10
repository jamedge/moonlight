package edge.jam.moonlight.core.model

import neotypes.Driver

import scala.concurrent.{ExecutionContext, Future}
import neotypes.implicits._

class LineService(neo4jDriver: Driver[Future])(implicit val executionContext: ExecutionContext) {
  def getAllCodeMetadata(): Future[Option[String]] = {
    val result = neo4jDriver.readSession { session =>
      c"""MATCH (code:Code) RETURN code
       """.query[Option[Code]].single(session)
    }

    result.map(_.map(_.toString))
  }
}
