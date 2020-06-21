package com.github.jamedge.moonlight.core.service.line

import com.github.jamedge.moonlight.core.model.Line
import com.github.jamedge.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement, Node, NodeClass, RelationshipRight}
import com.github.jamedge.moonlight.core.model.neo4j.queries.LineQueriesConstructor
import com.github.jamedge.moonlight.core.model.neo4j.{Nodes => N, Relationships => R}
import neotypes.{DeferredQuery, Driver, Transaction}
import shapeless.Id
import neotypes.implicits.all._
import org.neo4j.driver.v1.Value

import scala.concurrent.{ExecutionContext, Future}

class LinePersistenceLayer(
    neo4jDriver: Id[Driver[Future]]
)(implicit val executionContext: ExecutionContext) {
  /**
   * Adds an ETL line to Graph database.
   * <br/><br/>
   * Guarantees persistence of data entered in the line with additional data gained by merging with data from other lines.
   * Changing property values for selected `name` overrides previously set value - e.g. if same input has the same property
   * set twice, only the latter one will be persisted, as the db stores only the last state of the elements and their relationships.
   * <br/><br/>
   * Deletion of line doesn't clean the mutual properties with other lines (mutual node points) until they are still using them.
   * Properties of the node (and node itself) are deleted if the deleting entity is the last one using the node.
   *
   * @param line Line object that needs to be saved.
   * @return Future containing query result upon execution on neo4j db.
   */
  def addLine(implicit line: Line): Future[Unit] = {
    neo4jDriver.writeSession { implicit session =>
      session.transact[Unit] { implicit tx =>
        for {
          cd <- cleanupDownstream
          l <- createLineElements
          io <- createIOElements
          pb <- createProcessedByElements
          m <- createMetricsElements
          a <- createAlertsElements
          c <- createCodeElements
        } yield (cd, l, io, pb, m, a, c)
      }
    }
  }

  /**
   * Gets the line from the persistence layer for the specified line name.
   * @param lineName Name of the line.
   * @return Extracted line.
   */
  def getLine(lineName: String): Future[Line] = {
    neo4jDriver.readSession { implicit session =>
      session.transact[Line] { implicit tx =>
        for {
//          lineData <- LineQueries.
//            constructGetLineDataQuery(lineName).
//            query[RawLineDataRecord].list(tx)
          lineBase <- LineQueriesConstructor.matchNode(lineName).query[Option[Line]].single(tx)
          lineDetails <- LineQueriesConstructor.matchDetails(lineName, lineName).query[Option[Value]].single(tx)
          line <- Future(LineBuilder.buildLine(lineBase, lineDetails))
        } yield line
      }
    }
  }

  private def cleanupDownstream(implicit line:Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence(List(
      constructRelationshipDeleteMarking(line, N.Line(line)).execute(tx),
      constructDeleteCleanedRelationships().execute(tx),
      constructDeleteDetachedNodes(ElementClass.Details).execute(tx),
      constructDeleteDetachedNodes(ElementClass.IO).execute(tx),
      constructDeleteDetachedNodes(ElementClass.Storage).execute(tx),
      constructDeleteDetachedNodes(ElementClass.Alert).execute(tx),
      constructDeleteDetachedNodes(ElementClass.AlertsFramework).execute(tx),
      constructDeleteDetachedNodes(ElementClass.Metric).execute(tx),
      constructDeleteDetachedNodes(ElementClass.MetricsFramework).execute(tx),
      constructDeleteDetachedNodes(ElementClass.Process).execute(tx),
      constructDeleteDetachedNodes(ElementClass.ProcessingFramework).execute(tx),
      constructDeleteDetachedNodes(ElementClass.Code).execute(tx)
    ))
  }

  private def createLineElements(implicit line: Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence(
      List(
        constructCreateOrUpdateQuery(N.Line(line)).execute(tx),
        line.details.map(details =>
          constructCreateOrUpdateQuery(
            N.Line(line),
            Some(R.HasDetails()),
            Some(N.Details(details)),
            true).execute(tx)).getOrElse(Future.unit)))
  }

  private def createIOElements(implicit line: Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence(line.io.flatMap { io =>
      io.inputs.flatMap { input =>
        List(
          constructCreateOrUpdateQuery(
            N.Line(line),
            Some(R.HasInput()),
            Some(N.IO(input))).execute(tx),
          input.details.map(details =>
            constructCreateOrUpdateQuery(
              N.IO(input),
              Some(R.HasDetails()),
              Some(N.Details(details)),
              true).execute(tx)).getOrElse(Future.unit)) ++
          input.storage.map { storage =>
            List(
              constructCreateOrUpdateQuery(
                N.IO(input),
                Some(R.HasStorage()),
                Some(N.Storage(storage))).execute(tx),
              storage.details.map(details =>
                constructCreateOrUpdateQuery(
                  N.Storage(storage),
                  Some(R.HasDetails()),
                  Some(N.Details(details)),
                  true).execute(tx)).getOrElse(Future.unit))
          }.getOrElse(List())
        io.outputs.flatMap { output =>
          List(
            constructCreateOrUpdateQuery(
              N.IO(input),
              Some(R.HasOutput()),
              Some(N.IO(output))).execute(tx),
            output.details.map(details =>
              constructCreateOrUpdateQuery(
                N.IO(output),
                Some(R.HasDetails()),
                Some(N.Details(details)),
                true).execute(tx)).getOrElse(Future.unit)) ++
            output.storage.map { storage =>
              List(
                constructCreateOrUpdateQuery(
                  N.IO(output),
                  Some(R.HasStorage()),
                  Some(N.Storage(storage))).execute(tx),
                storage.details.map(details =>
                  constructCreateOrUpdateQuery(
                    N.Storage(storage),
                    Some(R.HasDetails()),
                    Some(N.Details(details)),
                    true).execute(tx)).getOrElse(Future.unit))
            }.getOrElse(List())
        }
      }
    })
  }

  private def createProcessedByElements(implicit line: Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence {
      line.processedBy.flatMap { process =>
        List(
          constructCreateOrUpdateQuery(
            N.Line(line),
            Some(R.IsProcessedBy()),
            Some(N.Process(process))).execute(tx),
          process.details.map(details =>
            constructCreateOrUpdateQuery(
              N.Process(process),
              Some(R.HasDetails()),
              Some(N.Details(details)),
              true).execute(tx)).getOrElse(Future.unit)) ++
          process.processingFramework.map { processingFramework =>
            List(
              constructCreateOrUpdateQuery(
                N.Process(process),
                Some(R.HasProcessingFramework()),
                Some(N.ProcessingFramework(processingFramework))).execute(tx),
              processingFramework.details.map(details =>
                constructCreateOrUpdateQuery(
                  N.ProcessingFramework(processingFramework),
                  Some(R.HasDetails()),
                  Some(N.Details(details)),
                  true).execute(tx)).getOrElse(Future.unit))
          }.getOrElse(List()) ++
          process.triggered.map { processingHistoryRecord =>
            List(
              constructCreateOrUpdateQuery(
                N.Process(process),
                Some(R.HasProcessingHistory()),
                Some(N.ProcessingHistory(processingHistoryRecord.processingHistory))).execute(tx),
              processingHistoryRecord.processingHistory.details.map(details =>
                constructCreateOrUpdateQuery(
                  N.ProcessingHistory(processingHistoryRecord.processingHistory),
                  Some(R.HasDetails()),
                  Some(N.Details(details)),
                  true).execute(tx)).getOrElse(Future.unit),
              constructCreateOrUpdateQuery(
                N.ProcessingHistory(processingHistoryRecord.processingHistory),
                Some(R.HasProcessingHistoryRecord(processingHistoryRecord.fieldsMap() + ("relationshipType" -> "permanent"))),
                Some(N.ProcessingHistory(processingHistoryRecord.processingHistory))).execute(tx),
            )
          }.getOrElse(List())
      }
    }
  }

  private def createMetricsElements(implicit line: Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence(line.metrics.flatMap { metric =>
      List(
        constructCreateOrUpdateQuery(
          N.Line(line),
          Some(R.HasMetrics()),
          Some(N.Metric(metric))).execute(tx),
        metric.details.map(details =>
          constructCreateOrUpdateQuery(
            N.Metric(metric),
            Some(R.HasDetails()),
            Some(N.Details(details)),
            true).execute(tx)).getOrElse(Future.unit)) ++
        metric.metricFramework.map { metricsFramework =>
          List(
            constructCreateOrUpdateQuery(
              N.Metric(metric),
              Some(R.HasMetricsFramework()),
              Some(N.MetricsFramework(metricsFramework))).execute(tx),
            metricsFramework.details.map(details =>
              constructCreateOrUpdateQuery(
                N.MetricsFramework(metricsFramework),
                Some(R.HasDetails()),
                Some(N.Details(details)),
                true).execute(tx)).getOrElse(Future.unit))
        }.getOrElse(List())
    })
  }

  private def createAlertsElements(implicit line: Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence(line.alerts.flatMap { alert =>
      List(
        constructCreateOrUpdateQuery(
          N.Line(line),
          Some(R.HasAlert()),
          Some(N.Alert(alert))).execute(tx),
        alert.details.map(details =>
          constructCreateOrUpdateQuery(
            N.Alert(alert),
            Some(R.HasDetails()),
            Some(N.Details(details)),
            true).execute(tx)).getOrElse(Future.unit)) ++
        alert.alertsFramework.map { alertsFramework =>
          List(
            constructCreateOrUpdateQuery(
              N.Alert(alert),
              Some(R.HasAlertsFramework()),
              Some(N.AlertsFramework(alertsFramework))).execute(tx),
            alertsFramework.details.map(details =>
              constructCreateOrUpdateQuery(
                N.AlertsFramework(alertsFramework),
                Some(R.HasDetails()),
                Some(N.Details(details)),
                true).execute(tx)).getOrElse(Future.unit))
        }.getOrElse(List())
    })
  }

  private def createCodeElements(implicit line: Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence(line.code.map { code =>
      List(
        constructCreateOrUpdateQuery(
          N.Line(line),
          Some(R.HasCode()),
          Some(N.Code(code))).execute(tx),
        code.details.map(details =>
          constructCreateOrUpdateQuery(
            N.Code(code),
            Some(R.HasDetails()),
            Some(N.Details(details)),
            true).execute(tx)).getOrElse(Future.unit))
    }.getOrElse(List()))
  }

  private def constructRelationshipDeleteMarking(
      line:Line,
      startNode: Node
  ): DeferredQuery[Unit] = {
    LineQueriesConstructor.prepareRelationshipsForDeletion("fromLines", line.name, startNode).query[Unit]
  }

  private def constructDeleteCleanedRelationships(): DeferredQuery[Unit] = {
    LineQueriesConstructor.deleteCleanedRelationships().query[Unit]
  }

  private def constructDeleteDetachedNodes(nodeClass: NodeClass): DeferredQuery[Unit] = {
    LineQueriesConstructor.deleteDetachedNodesQuery(nodeClass).query[Unit]
  }

  private def constructCreateOrUpdateQuery(
      n1: Node,
      r: Option[RelationshipRight] = None,
      n2: Option[Node] = None,
      createDuplicateNode2IfPathNotFound: Boolean = false,
  )(implicit line: Line): DeferredQuery[Unit] = {
    LineQueriesConstructor.createOrUpdate(
      n1,
      r,
      n2,
      createDuplicateNode2IfPathNotFound,
      None,
      r.map(LineQueriesConstructor.snippetRelationshipTagging("fromLines", line.name, _))).query[Unit]
  }
}
