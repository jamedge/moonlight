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

  def addLine(line: Line): Future[Unit] = {
    neo4jDriver.writeSession { session =>
      c"".+(lineNodeQuery(line))
        .+(detailsQuery("l", line.details.getOrElse(Map())))
        .query[Unit].execute(session)
    }
  }

  def lineNodeQuery(line: Line): String = {
    val lineFields = Map(
      "name" -> line.name,
      "owner" -> line.owner.getOrElse(""),
      "purpose" -> line.purpose.getOrElse(""),
      "notes" -> s"[${line.notes.getOrElse(List()).map(el => s""""$el"""").mkString(", ")}]"
    )
    val a = s"MERGE ${Nodes.Line(lineFields)}"
    println(a)
    a
  }

  def detailsQuery(parentVariable: String, details: Map[String, String]): String = {
    val a = s"MERGE ($parentVariable) ${Relationships.HasDetails()} ${Nodes.LineDetails(details)}"
    println(a)
    a
  }

}
