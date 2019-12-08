package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.neo4j.GraphElements
import edge.jam.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}
import neotypes.{DeferredQuery, Driver, Transaction}

import scala.concurrent.{ExecutionContext, Future}
import neotypes.implicits.all._
import org.slf4j.Logger
import edge.jam.moonlight.core.model.neo4j.{Nodes => N, Relationships => R}
import shapeless._

class LineService(
    neo4jDriver: Id[Driver[Future]],
    logger: Logger
)(implicit val executionContext: ExecutionContext) {

  def getAllCodeMetadata(): Future[Option[String]] = {
      val result = neo4jDriver.readSession { session =>
        "MATCH (code:Code) RETURN code".query[Option[Code]].single(session)
      }
      result.map(_.map(_.toString))
  }

  /**
   * Adds an ETL line to Graph database.
   * <br/><br/>
   * Guarantees persistence of data entered in the line with additional data gained by merging with data from other lines.
   * Changing property values for selected `name` overrides previously set value - e.g. if same input has the same property
   * set twice, only the latter one will be persisted, as the db stores only the last state of the elements and their relationships.
   * <br/><br/>
   * Deletion of line doesn't clean the mutual properties with other lines (mutual node points) until they are using them.
   * Properties of the node (and node itself) are deleted if the deleting entity is the last one using the node.
   *
   * @param line Line object that needs to be saved.
   * @return Future containing query executions on neo4j db.
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

  private def cleanupDownstream(implicit line:Line, tx: Transaction[Future]): Future[List[Unit]] = {
    Future.sequence(List(
      constructRelationshipDeleteMarking(line, N.Line(line)).execute(tx),
      constructDeleteCleanedRelationships().execute(tx),
      constructDeleteDetachedNodes(ElementClass.Line).execute(tx),
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
            true).execute(tx)).getOrElse(Future())))
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
              true).execute(tx)).getOrElse(Future())) ++
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
                  true).execute(tx)).getOrElse(Future()))
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
                true).execute(tx)).getOrElse(Future())) ++
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
                    true).execute(tx)).getOrElse(Future()))
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
              true).execute(tx)).getOrElse(Future()),
          process.processingFramework.map { processingFramework =>
            constructCreateOrUpdateQuery(
              N.Process(process),
              Some(R.HasProcessingFramework()),
              Some(N.ProcessingFramework(processingFramework))).execute(tx)
            processingFramework.details.map(details =>
              constructCreateOrUpdateQuery(
                N.ProcessingFramework(processingFramework),
                Some(R.HasDetails()),
                Some(N.Details(details)),
                true).execute(tx)).getOrElse(Future())
          }.getOrElse(Future()))
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
            true).execute(tx)).getOrElse(Future())) ++
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
                true).execute(tx)).getOrElse(Future()))
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
            true).execute(tx)).getOrElse(Future())) ++
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
                true).execute(tx)).getOrElse(Future()))
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
            true).execute(tx)).getOrElse(Future()))
    }.getOrElse(List()))
  }

  private def constructRelationshipDeleteMarking(
      line:Line,
      startElement: GraphElement
  ): DeferredQuery[Unit] = {
    val query = GraphElements.constructRelationshipDeleteMarkingQuery("fromLines", line.name, startElement).query[Unit]
    logQueryCreation(query)
    query
  }

  private def constructDeleteCleanedRelationships(): DeferredQuery[Unit] = {
    val query = GraphElements.constructDeleteCleanedRelationshipsQuery().query[Unit]
    logQueryCreation(query)
    query
  }

  private def constructDeleteDetachedNodes(elementClass: ElementClass): DeferredQuery[Unit] = {
    val query = GraphElements.constructDeleteDetachedNodesQuery(elementClass).query[Unit]
    logQueryCreation(query)
    query
  }

  private def constructCreateOrUpdateQuery(
      n1: GraphElement,
      r: Option[GraphElement] = None,
      n2: Option[GraphElement] = None,
      createDuplicateNode2IfPathNotFound: Boolean = false,
  )(implicit line: Line): DeferredQuery[Unit] = {
    val query = GraphElements.constructCreateOrUpdateQuery(
        n1,
        r,
        n2,
        createDuplicateNode2IfPathNotFound,
        None,
        r.map(GraphElements.constructRelationshipTaggingSnippet("fromLines", line.name, _))).query[Unit]
    logQueryCreation(query)
    query
  }

  private def logQueryCreation(query: DeferredQuery[_]): Unit = {
    logger.debug(s"Creating query:\n${query.query}\nwith params:\n${query.params.map(p => s"${p._1}: ${p._2.toString}")}")
  }
}
