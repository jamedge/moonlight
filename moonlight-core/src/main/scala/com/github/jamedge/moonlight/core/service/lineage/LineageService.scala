package com.github.jamedge.moonlight.core.service.lineage

import scala.concurrent.{ExecutionContext, Future}

class LineageService(
    persistenceLayer: PersistenceLayer,
    graphFormatter: GraphFormatter
)(implicit val executionContext: ExecutionContext) {
  /**
   * Gets json representation of the lineage graph made from downstream IO Elements which is
   * fetched from the persistence layer for the specified root element.
   *
   * @param rootIOElementName Value of the `name` attribute of the root IO Element.
   * @return Json representation of the graph containing all IO Elements downstream from the root IO Element.
   *         Resulting json object represents graph by 2 fields: `nodes` and `edges`.
   */
  def getLineageGraphJson(rootIOElementName: String): Future[String] = {
    import scalax.collection.io.json._
    for {
      graph <- persistenceLayer.getLineageGraph(rootIOElementName)
      resultJson <- Future(graph.toJson(GraphBuilder.createLineageGraphJsonDescriptor()))
    } yield resultJson
  }

  /**
   * Gets formatted representation of the lineage graph made from downstream IO Elements which is
   * fetched from the persistence layer for the specified root element.
   *
   * @param rootIOElementName Value of the `name` attribute of the root IO Element.
   * @param outputType Desired format of output representation.
   * @return Formatted representation of the graph containing all IO Elements downstream from the root IO Element.
   *         Result presents the graph as a formatted tree of IO Elements.
   */
  def getLineageGraphFormatted(rootIOElementName: String, outputType: LineageGraphFormattedOutputType): Future[String] = {
    for {
      graph <- persistenceLayer.getLineageGraph(rootIOElementName)
      resultJson <- Future(graphFormatter.formatLineageGraph(graph, rootIOElementName, outputType))
    } yield resultJson
  }
}
