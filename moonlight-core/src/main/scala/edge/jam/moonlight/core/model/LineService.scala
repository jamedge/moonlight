package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.neo4j.GraphElements
import edge.jam.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}
import neotypes.{DeferredQuery, DeferredQueryBuilder, Driver, GraphDatabase, Transaction}

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

  // Guarantees data entered in a line with additional data gained by merging with data from other lines
  // Deletion of line doesn't clean the mutual properties until there are other lines using them.
  // They will override these properties if they are the only owners of the node or if they do it explicitly
  // TODO: try to remove the properties from mutual nodes during cleanup
  // TODO: there needs to be a way of knowing which property belongs to which Line (is this that important?). Maybe a merge strategy can be chosen externally (merge or overwrite)
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

  // TODO: extract all DDL keywords to GraphElements
  private def constructRelationshipDeleteMarking(
      line:Line,
      startElement: GraphElement
  ): DeferredQuery[Unit] = {
    val query = (
      c"MATCH p =" +  startElement.toSearchObject() + "-[*0..]-> (x)" +
      c"FOREACH ( x IN relationships(p) | SET x.fromLines = FILTER ( y IN x.fromLines WHERE y <> ${line.name}))").query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  private def constructDeleteCleanedRelationships(): DeferredQuery[Unit] = {
    val query =
      c"MATCH (i) <-[s {fromLines: []}]- () DELETE s".query[Unit]
    logQueryCreation(query)
    query
  }

  // TODO: extract all DDL keywords to GraphElements
  private def constructDeleteDetachedNodes(elementClass: ElementClass): DeferredQuery[Unit] = {
    val query =
      (c"MATCH" + s"(i:${elementClass.name})" + c"WHERE NOT (i) <-- () DELETE i").query[Unit]
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
        r.map(lineTaggingSnippet(line, _))).query[Unit]
    logQueryCreation(query)
    query
  }

  private def lineTaggingSnippet(line: Line, relationship: GraphElement): DeferredQueryBuilder = {
    c"ON MATCH SET (CASE WHEN NOT ${line.name} IN" + relationship.toVariableWithNewField("fromLines") +
      c"THEN" + relationship.toVariable() + c"END).fromLines =" + relationship.toVariableWithNewField("fromLines") + c" + [${line.name}]" +
      c"ON CREATE SET" + relationship.toVariableWithNewField("fromLines") + c"= [${line.name}]"
  }

  private def logQueryCreation(query: DeferredQuery[_]): Unit = {
    logger.debug(s"Creating query:\n${query.query}\nwith params:\n${query.params.map(p => s"${p._1}: ${p._2.toString}")}")
  }
}
