package com.github.jamedge.moonlight.core.service.line

import com.github.jamedge.moonlight.core.model.{Alert, AlertsFramework, Code, Line, Metric, MetricsFramework, Process, ProcessingFramework, ProcessingHistoryRecord}
import com.github.jamedge.moonlight.core.model.neo4j.{ElementClass, Node, NodeClass, RelationshipRight, Nodes => N, Relationships => R}
import com.github.jamedge.moonlight.core.model.neo4j.queries.{ChainLink, LineQueriesConstructor, DefaultNodePresent => DNP, DefaultRelationshipRightPresent => DRRP}
import com.github.jamedge.moonlight.core.service.line.LineBuilder.ProcessingHistoryRecordLight
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
  // TODO: break this into smaller methods after it's done with the whole line
  def getLine(lineName: String): Future[Line] = {
    neo4jDriver.readSession { implicit session =>
      session.transact[Line] { implicit tx =>
        for {
          lineBase <- LineQueriesConstructor.matchNode(lineName).query[Option[Line]].single(tx)
          lineDetails <- LineQueriesConstructor.matchDetails(lineName, lineName).query[Option[Value]].single(tx)
          processedBy <- LineQueriesConstructor.matchConnectingNode(
            lineName, R.IsProcessedBy(), N.Process("p"), lineName
          ).query[Process].list(tx)
          processedByDetails <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.IsProcessedBy(), N.Process("p")),
              ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)), // TODO: examine how does it work /wt unstructured param (it seems it work correctly /wt it as well)
            ), lineName).query[(Process, Value)].map(tx)
          processingFrameworks <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.IsProcessedBy(), N.Process("p")),
              ChainLink(R.HasProcessingFramework(), N.ProcessingFramework("pf")),
            ), lineName).query[(Process, ProcessingFramework)].map(tx)
          processingFrameworksDetails <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.IsProcessedBy(), DNP(N.Process("p"), show = false)),
              ChainLink(R.HasProcessingFramework(), N.ProcessingFramework("pf")),
              ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
            ), lineName).query[(ProcessingFramework, Value)].map(tx)
          metrics <- LineQueriesConstructor.matchConnectingNode(
            lineName, R.HasMetrics(), N.Metric("m"), lineName
          ).query[Metric].list(tx)
          metricsDetails <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.HasMetrics(), N.Metric("m")),
              ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
            ), lineName).query[(Metric, Value)].map(tx)
          metricsFrameworks <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.HasMetrics(), N.Metric("m")),
              ChainLink(R.HasMetricsFramework(), N.MetricsFramework("mf")),
            ), lineName).query[(Metric, MetricsFramework)].map(tx)
          metricsFrameworksDetails <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.HasMetrics(), DNP(N.Metric("m"), show = false)),
              ChainLink(R.HasMetricsFramework(), N.MetricsFramework("mf")),
              ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
            ), lineName).query[(MetricsFramework, Value)].map(tx)
          alerts <- LineQueriesConstructor.matchConnectingNode(
            lineName, R.HasAlert(), N.Alert("a"), lineName
          ).query[Alert].list(tx)
          alertsDetails <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.HasAlert(), N.Alert("a")),
              ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
            ), lineName).query[(Alert, Value)].map(tx)
          alertsFrameworks <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.HasAlert(), N.Alert("a")),
              ChainLink(R.HasAlertsFramework(), N.AlertsFramework("af")),
            ), lineName).query[(Alert, AlertsFramework)].map(tx)
          alertsFrameworksDetails <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.HasAlert(), DNP(N.Alert("a"), show = false)),
              ChainLink(R.HasAlertsFramework(), N.AlertsFramework("af")),
              ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
            ), lineName).query[(AlertsFramework, Value)].map(tx)
          code <- LineQueriesConstructor.matchConnectingNode(
            lineName, R.HasCode(), N.Code("c"), lineName
          ).query[Option[Code]].single(tx)
          codeDetails <- LineQueriesConstructor.matchConnectingChain(
            lineName, List(
              ChainLink(R.HasCode(), N.Code("c")),
              ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
            ), lineName).query[(Code, Value)].map(tx)
          line <- Future(LineBuilder.buildLine(
            lineBase,
            lineDetails,
            processedBy,
            processedByDetails.map { case (p: Process, d: Value) => (p.name, d)},
            processingFrameworks.map { case (p: Process, pf: ProcessingFramework) => (p.name, pf)},
            processingFrameworksDetails.map { case (p: ProcessingFramework, d: Value) => (p.name, d)},
            metrics,
            metricsDetails.map { case (m: Metric, d: Value) => (m.name, d)},
            metricsFrameworks.map { case (m: Metric, mf: MetricsFramework) => (m.name, mf)},
            metricsFrameworksDetails.map { case (mf: MetricsFramework, d: Value) => (mf.name, d)},
            alerts,
            alertsDetails.map { case (a: Alert, d: Value) => (a.name, d)},
            alertsFrameworks.map { case (a: Alert, af: AlertsFramework) => (a.name, af)},
            alertsFrameworksDetails.map { case (af: AlertsFramework, d: Value) => (af.name, d)},
            code,
            codeDetails.map { case (c: Code, d: Value) => (c.name, d)}
          ))
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
