package edge.jam.moonlight.core.model

import edge.jam.moonlight.core.model.neo4j.GraphElements
import edge.jam.moonlight.core.model.neo4j.GraphElements.{ElementClass, GraphElement}
import neotypes.{DeferredQuery, Driver, Transaction}

import scala.concurrent.{ExecutionContext, Future}
import neotypes.implicits.all._
import org.slf4j.Logger
import edge.jam.moonlight.core.model.neo4j.{Nodes => N, Relationships => R}
import org.neo4j.driver.v1.{Value, Values}

import shapeless._
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import scalax.collection.io.json.descriptor.predefined.LDi
import scalax.collection.io.json.{Descriptor, NodeDescriptor}

class LineService(
    neo4jDriver: Id[Driver[Future]],
    logger: Logger
)(implicit val executionContext: ExecutionContext) {

  case class RawEdge(left: IOElement, properties: List[String], right: IOElement)

  def getAllCodeMetadata(): Future[Option[String]] = {

    val resultEdges = neo4jDriver.readSession { session =>
      """MATCH p = (i:IO {name:'mi1'}) -[:HAS_OUTPUT *1..]-> (n:IO)
        |WITH p
        |MATCH (a) -[r]-> (b) WHERE r IN relationships(p)
        |RETURN DISTINCT a AS left, r.fromLines AS properties, b AS right""".stripMargin.
        query[RawEdge].list(session)
    }

    val resultIODetails = neo4jDriver.readSession { session =>
      """MATCH (i:IO) -[:HAS_DETAILS]-> (d)
        |RETURN DISTINCT i.name, d {.*}""".stripMargin.
        query[(String, Value)].map(session)
    }

    val resultStorage = neo4jDriver.readSession { session =>
      """MATCH (i:IO) -[:HAS_STORAGE]-> (s)
        |RETURN i.name, s""".stripMargin.
        query[(String, Storage)].map(session)
    }

    val resultStorageDetails = neo4jDriver.readSession { session =>
      """MATCH (s:Storage) -[:HAS_DETAILS]-> (d)
        |RETURN DISTINCT s.name, d {.*}""".stripMargin.
        query[(String, Value)].map(session)
    }

    val ioDescriptor = new NodeDescriptor[IOElement](typeId = "IOs") {
      def id(node: Any) = node match {
        case IOElement(name, owner, purpose, notes, details, storage, locationRelativePath) => name
      }
    }
    val quickJson = new Descriptor[IOElement](
      defaultNodeDescriptor = ioDescriptor,
      defaultEdgeDescriptor = LDi.descriptor[IOElement, String]("lines"),
      namedNodeDescriptors = Seq(ioDescriptor),
      namedEdgeDescriptors = Seq(LDi.descriptor[IOElement, String]("lines"))
    )

    resultEdges.flatMap { rawEdges =>
      resultIODetails.flatMap { rawIODetails =>
        resultStorage.flatMap { storages =>
          resultStorageDetails.map { rawStorageDetails =>
            val edges = rawEdges.map { rawEdge =>
              val left = buildIOElement(rawEdge.left, rawIODetails, storages, rawStorageDetails)
              val right = buildIOElement(rawEdge.right, rawIODetails, storages, rawStorageDetails)
              LDiEdge(left, right)(rawEdge.properties)
            }
            val g = Graph[IOElement, LDiEdge](edges: _*)
            import scalax.collection.io.json._
            Some(g.toJson(quickJson))
          }
        }
      }
    }
  }

  private def buildIOElement(
      ioElement: IOElement,
      rawIODetails: Map[String, Value],
      storages: Map[String, Storage],
      rawStorageDetails: Map[String, Value]
  ): IOElement = {
    ioElement.copy(
      storage = storages.get(ioElement.name).map(s => s.copy(
        details = extractDetailsFromGraphValues(rawStorageDetails, s.name))),
      details = extractDetailsFromGraphValues(rawIODetails, ioElement.name))
  }

  private def extractDetailsFromGraphValues(values: Map[String, Value], parentName: String): Option[Map[String, String]] = {
    import scala.collection.JavaConverters._
    values.get(parentName).map(_.asMap[String](Values.ofString()).asScala.toMap)
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
              true).execute(tx)).getOrElse(Future())) ++
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
                  true).execute(tx)).getOrElse(Future()))
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
                true).execute(tx)).getOrElse(Future()),
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
