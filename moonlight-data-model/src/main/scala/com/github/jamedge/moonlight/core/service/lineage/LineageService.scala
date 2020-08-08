package com.github.jamedge.moonlight.core.service.lineage

import com.github.jamedge.moonlight.core.model.IOElement
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

import scala.concurrent.{ExecutionContext, Future}

class LineageService(
    persistenceLayer: LineagePersistenceLayer
)(implicit val executionContext: ExecutionContext) {
  /**
   * Gets lineage graph made from downstream IO Elements which is
   * fetched from the persistence layer for the specified root element.
   *
   * @param rootIOElementName Value of the `name` attribute of the root IO Element.
   * @return Graph containing all IO Elements downstream from the root IO Element.
   *         Resulting object represents graph by 2 fields: `nodes` and `edges`.
   */
  def getLineageGraph(rootIOElementName: String): Future[Graph[IOElement, LDiEdge]] = {
    persistenceLayer.getLineageGraph(rootIOElementName)
  }
}
