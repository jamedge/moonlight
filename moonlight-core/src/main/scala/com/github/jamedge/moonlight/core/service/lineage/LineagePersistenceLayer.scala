package com.github.jamedge.moonlight.core.service.lineage

import com.github.jamedge.moonlight.core.model.neo4j.queryconstructors.LineageQueriesConstructor
import com.github.jamedge.moonlight.core.model.{IOElement, Storage}
import neotypes.Driver
import org.neo4j.driver.v1.Value
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import shapeless.Id
import neotypes.implicits.all._

import scala.concurrent.{ExecutionContext, Future}

class LineagePersistenceLayer(
    neo4jDriver: Id[Driver[Future]]
)(implicit val executionContext: ExecutionContext)  {
  /**
   * Gets lineage graph made from downstream IO Elements which is
   * fetched from the persistence layer for the specified root element.
   *
   * @param rootIOElementName Value of the `name` attribute of the root IO Element.
   * @return Graph containing all IO Elements downstream from the root IO Element.
   */
  def getLineageGraph(rootIOElementName: String): Future[Graph[IOElement, LDiEdge]] = {
    neo4jDriver.readSession { implicit session =>
      session.transact[Graph[IOElement, LDiEdge]] { implicit tx =>
        for {
          rawEdges <- LineageQueriesConstructor.constructGetLineageRawEdgesQuery(rootIOElementName).query[GraphBuilder.RawEdge].list(tx)
          rawIODetails <- LineageQueriesConstructor.constructGetAllIODetailsQuery().query[(String, Value)].map(tx)
          storages <- LineageQueriesConstructor.constructGetAllIOStoragesQuery().query[(String, Storage)].map(tx)
          rawStorageDetails <- LineageQueriesConstructor.constructGetAllStorageDetailsQuery().query[(String, Value)].map(tx)
          graph <- Future(GraphBuilder.buildLineageGraph(rawEdges, rawIODetails, storages, rawStorageDetails))
        } yield graph
      }
    }
  }
}
