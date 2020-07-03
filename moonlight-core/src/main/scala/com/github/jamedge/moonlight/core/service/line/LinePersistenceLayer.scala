package com.github.jamedge.moonlight.core.service.line

import com.github.jamedge.moonlight.core.model.{Alert, AlertsFramework, Code, IOElement, Line, Metric, MetricsFramework, Process, ProcessingFramework, ProcessingHistoryRecord, Storage}
import com.github.jamedge.moonlight.core.model.neo4j.{ElementClass, Node, NodeClass, RelationshipRight, Nodes => N, Relationships => R}
import com.github.jamedge.moonlight.core.model.neo4j.queries.{ChainLink, LineQueriesConstructor, DefaultNodePresent => DNP, DefaultRelationshipRightPresent => DRRP}
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
          (ioPairs, inputsDetails, inputsStorage, inputsStorageDetails) <- matchInputsData(lineName)
          (outputsDetails, outputsStorage, outputsStorageDetails) <- matchOutputsData(lineName)
          (processedBy, processedByDetails, processingFrameworks, processingFrameworksDetails) <- matchProcessData(lineName)
          (metrics, metricsDetails, metricsFrameworks, metricsFrameworksDetails) <- matchMetricsData(lineName)
          (alerts, alertsDetails, alertsFrameworks, alertsFrameworksDetails) <- matchAlertsData(lineName)
          (code, codeDetails) <- matchCodeData(lineName)
          line <- Future(LineBuilder.buildLine(
            lineBase,
            lineDetails,
            ioPairs,
            inputsDetails,
            inputsStorage,
            inputsStorageDetails,
            outputsDetails,
            outputsStorage,
            outputsStorageDetails,
            processedBy,
            processedByDetails,
            processingFrameworks,
            processingFrameworksDetails,
            metrics,
            metricsDetails,
            metricsFrameworks,
            metricsFrameworksDetails,
            alerts,
            alertsDetails,
            alertsFrameworks,
            alertsFrameworksDetails,
            code,
            codeDetails
          ))
        } yield line
      }
    }
  }

  private def matchInputsData(lineName: String)(implicit tx: Transaction[Future]):
    Future[(List[(IOElement, IOElement)], Map[String, Value], Map[String, Storage], Map[String, Value])] = {
    for {
      ioPairs <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasInput(), N.IO("i")),
          ChainLink(R.HasOutput(), N.IO("o")),
        ), lineName).query[(IOElement, IOElement)].list(tx)
      inputsDetails <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasInput(), N.IO("i")),
          ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
        ), lineName).query[(IOElement, Value)].map(tx)
      inputsStorage <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasInput(), N.IO("i")),
          ChainLink(R.HasStorage(), N.Storage("s")),
        ), lineName).query[(IOElement, Storage)].map(tx)
      inputsStorageDetails <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasInput(), DNP(N.IO("i"), show = false)),
          ChainLink(R.HasStorage(), N.Storage("s")),
          ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
        ), lineName).query[(Storage, Value)].map(tx)
    } yield (
      ioPairs,
      inputsDetails.map { case (i: IOElement, d: Value) => (i.name, d)},
      inputsStorage.map { case (i: IOElement, s: Storage) => (i.name, s)},
      inputsStorageDetails.map { case (s: Storage, d: Value) => (s.name, d)})
  }

  private def matchOutputsData(lineName: String)(implicit tx: Transaction[Future]):
  Future[(Map[String, Value], Map[String, Storage], Map[String, Value])] = {
    for {
      outputsDetails <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasInput(), DNP(N.IO("i"), show = false)),
          ChainLink(R.HasOutput(), N.IO("o")),
          ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
        ), lineName).query[(IOElement, Value)].map(tx)
      outputsStorage <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasInput(), DNP(N.IO("i"), show = false)),
          ChainLink(R.HasOutput(), N.IO("o")),
          ChainLink(R.HasStorage(), N.Storage("s")),
        ), lineName).query[(IOElement, Storage)].map(tx)
      outputsStorageDetails <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasInput(), DNP(N.IO("i"), show = false)),
          ChainLink(R.HasOutput(), DNP(N.IO("o"), show = false)),
          ChainLink(R.HasStorage(), N.Storage("s")),
          ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
        ), lineName).query[(Storage, Value)].map(tx)
    } yield (
      outputsDetails.map { case (i: IOElement, d: Value) => (i.name, d)},
      outputsStorage.map { case (i: IOElement, s: Storage) => (i.name, s)},
      outputsStorageDetails.map { case (s: Storage, d: Value) => (s.name, d)})
  }

  private def matchProcessData(lineName: String)(implicit tx: Transaction[Future]):
  Future[(List[Process], Map[String, Value], Map[String, ProcessingFramework], Map[String, Value])] = {
    for {
      processedBy <- LineQueriesConstructor.matchConnectingNode(
        lineName, R.IsProcessedBy(), N.Process("p"), lineName
      ).query[Process].list(tx)
      processedByDetails <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.IsProcessedBy(), N.Process("p")),
          ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
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
    } yield (
      processedBy,
      processedByDetails.map { case (p: Process, d: Value) => (p.name, d)},
      processingFrameworks.map { case (p: Process, pf: ProcessingFramework) => (p.name, pf)},
      processingFrameworksDetails.map { case (p: ProcessingFramework, d: Value) => (p.name, d)})
  }

  private def matchMetricsData(lineName: String)(implicit tx: Transaction[Future]):
  Future[(List[Metric], Map[String, Value], Map[String, MetricsFramework], Map[String, Value])] = {
    for {
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
    } yield (
      metrics,
      metricsDetails.map { case (m: Metric, d: Value) => (m.name, d)},
      metricsFrameworks.map { case (m: Metric, mf: MetricsFramework) => (m.name, mf)},
      metricsFrameworksDetails.map { case (mf: MetricsFramework, d: Value) => (mf.name, d)})
  }

  private def matchAlertsData(lineName: String)(implicit tx: Transaction[Future]):
  Future[(List[Alert], Map[String, Value], Map[String, AlertsFramework], Map[String, Value])] = {
    for {
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
    } yield (
      alerts,
      alertsDetails.map { case (a: Alert, d: Value) => (a.name, d)},
      alertsFrameworks.map { case (a: Alert, af: AlertsFramework) => (a.name, af)},
      alertsFrameworksDetails.map { case (af: AlertsFramework, d: Value) => (af.name, d)})
  }

  private def matchCodeData(lineName: String)(implicit tx: Transaction[Future]):
  Future[(Option[Code], Map[String, Value])] = {
    for {
      code <- LineQueriesConstructor.matchConnectingNode(
        lineName, R.HasCode(), N.Code("c"), lineName
      ).query[Option[Code]].single(tx)
      codeDetails <- LineQueriesConstructor.matchConnectingChain(
        lineName, List(
          ChainLink(R.HasCode(), N.Code("c")),
          ChainLink(R.HasDetails(), DNP(N.Details(), unstructured = true)),
        ), lineName).query[(Code, Value)].map(tx)
    } yield (
      code,
      codeDetails.map { case (c: Code, d: Value) => (c.name, d)})
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
