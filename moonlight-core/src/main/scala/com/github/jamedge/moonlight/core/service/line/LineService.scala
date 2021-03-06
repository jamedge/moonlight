package com.github.jamedge.moonlight.core.service.line

import com.github.jamedge.moonlight.core.model.Line

import scala.concurrent.{ExecutionContext, Future}

class LineService(
    persistenceLayer: LinePersistenceLayer
)(implicit val executionContext: ExecutionContext) {
  /**
   * Adds an ETL line to Graph database.
   *
   * @param line Line object that needs to be saved.
   * @return Future containing query result upon execution on neo4j db.
   */
  def addLine(implicit line: Line): Future[Unit] = {
    persistenceLayer.addLine(line)
  }

  /**
   * Gets an ETL line from the Graph database.
   *
   * @param lineName Name of the line that needs to be pulled form the graph database.
   * @return Future containing query result upon execution on neo4j db.
   */
  def getLine(lineName: String): Future[Option[Line]] = {
    persistenceLayer.getLine(lineName)
  }

  /**
   * Gets all ETL lines from the Graph database.
   *
   * @return Future containing query result upon execution on neo4j db.
   */
  def getLines: Future[List[Line]] = {
    persistenceLayer.getLines
  }
}
