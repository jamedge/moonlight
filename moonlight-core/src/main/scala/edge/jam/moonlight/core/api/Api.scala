package edge.jam.moonlight.core.api

import akka.actor.ActorSystem
import org.slf4j.Logger
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Route
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read
import edge.jam.moonlight.core.model._

import scala.concurrent.{ExecutionContext, Future}

class Api(
    logger: Logger,
    apiConfig: ApiConfig,
    lineService: LineService
) (implicit val executionContext: ExecutionContext, actorSystem: ActorSystem) {
  private implicit val formats = DefaultFormats

  import akka.http.scaladsl.server.Directives._

  val route =
    handleExceptions(ExceptionHandlerBuilder.build()(logger)) {
      post {
        path("addLine") {
          entity(as[String]) { requestBody => addLine(requestBody) }
        }
      } ~
        get {
          path("status") {
            complete("OK")
          }
        } ~
        get {
          path("lineage" / "graph") {
            parameters("root_io") { rootIOElementName =>
              complete {
                getLineageGraph(rootIOElementName).map { graphJson =>
                  HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, graphJson))
                }
              }
            }
          }
        } ~
        get {
          path("lineage" / "graph" / "md") { // TODO: distinguish this vs json output based on content type
            parameters("root_io") { rootIOElementName =>
              complete {
                getLineageGraphDownstream(rootIOElementName, LineageGraphDownstreamOutputType.Md).map { graphMd =>
                  HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, graphMd))
                }
              }
            }
          }
        } ~
        get {
          path("lineage" / "graph" / "json") {
            parameters("root_io") { rootIOElementName =>
              complete {
                getLineageGraphDownstream(rootIOElementName, LineageGraphDownstreamOutputType.Json).map { graphJson =>
                  HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, graphJson))
                }
              }
            }
          }
        }
    }
  logger.info("Route initialized.")

  def init(): Unit = {
    logger.info("Initialising API")
    Http().bindAndHandle(
      route,
      apiConfig.server.host,
      apiConfig.server.port
    )
    logger.info("Server started.")
  }

  def getLineageGraph(rootIOElementName: String): Future[String] = {
    lineService.getLineageGraphJson(rootIOElementName)
  }

  def getLineageGraphDownstream(rootIOElementName: String, outputType: LineageGraphDownstreamOutputType): Future[String] = {
    lineService.getLineageGraphDownstream(rootIOElementName, outputType)
  }

  def addLine(requestJson: String): Route = {
    val line = read[Line](requestJson)
    val result = lineService.addLine(line).map(r => "Line added/updated.")
    complete(result)
  }
}

object Api {
  val module = new Module("moonlight-core")

  def main(args: Array[String]): Unit = {
    module.api.init()
  }
}
