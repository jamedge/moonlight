package com.github.jamedge.moonlight.core.service.line

import com.github.jamedge.moonlight.core.model.{Alert, AlertsFramework, Code, IO, IOElement, Line, Metadata, Metric, MetricsFramework, Process, ProcessingFramework, ProcessingHistoryRecord, Storage}
import org.neo4j.driver.v1.{Value, Values}

object LineBuilder {
  // TODO: build up this function to create the whole line
  def buildLine(
      line: Option[Line],
      lineDetails: Option[Value],
      ioPairs: List[(IOElement, IOElement)],
      inputsDetails: Map[String, Value],
      inputsStorage: Map[String, Storage],
      inputsStorageDetails: Map[String, Value],
      outputsDetails: Map[String, Value],
      outputsStorage: Map[String, Storage],
      outputsStorageDetails: Map[String, Value],
      processedBy: List[Process],
      processedByDetails: Map[String, Value],
      processingFrameworks: Map[String, ProcessingFramework],
      processingFrameworksDetails: Map[String, Value],
      metrics: List[Metric],
      metricsDetails: Map[String, Value],
      metricsFrameworks: Map[String, MetricsFramework],
      metricsFrameworksDetails: Map[String, Value],
      alerts: List[Alert],
      alertsDetails: Map[String, Value],
      alertsFrameworks: Map[String, AlertsFramework],
      alertsFrameworksDetails: Map[String, Value],
      code: Option[Code],
      codeDetails: Map[String, Value]
  ): Option[Line] = {
    line.map{ l =>
      l.copy(
        details = extractDetails(lineDetails),
        io = {
          ioPairs.
            groupBy { case (_, output: IOElement) => output }.
            map[IOElement, List[IOElement]] { case (output: IOElement, pairs:List[(IOElement, IOElement)]) => (output, pairs.map(_._1))}.
            groupBy { case (_, input: List[IOElement]) => input}.
            toList.
            map { case (inputs, pairs) => IO(
                inputs = inputs.map { i =>
                  i.copy(
                    details = extractDetails(inputsDetails.get(i.name)),
                    storage = inputsStorage.get(i.name).map { is =>
                      is.copy(details = extractDetails(inputsStorageDetails.get(is.name)))
                    }
                  )
                },
                outputs = pairs.iterator.map(_._1).toList.map { o =>
                  o.copy(
                    details = extractDetails(outputsDetails.get(o.name)),
                    storage = outputsStorage.get(o.name).map { os =>
                      os.copy(details = extractDetails(outputsStorageDetails.get(os.name)))
                    }
                  )
                }
              )
            }
        },
        processedBy = processedBy.map { pb =>
          pb.copy(
            details = extractDetails(processedByDetails.get(pb.name)),
            processingFramework = processingFrameworks.get(pb.name).map { pf =>
              pf.copy(details = extractDetails(processingFrameworksDetails.get(pf.name)))
            }
          )
        },
        metrics = metrics.map { m =>
          m.copy(
            details = extractDetails(metricsDetails.get(m.name)),
            metricFramework = metricsFrameworks.get(m.name).map { mf =>
              mf.copy(details = extractDetails(metricsFrameworksDetails.get(mf.name)))
            }
          )
        },
        alerts = alerts.map { a =>
          a.copy(
            details = extractDetails(alertsDetails.get(a.name)),
            alertsFramework = alertsFrameworks.get(a.name).map { af =>
              af.copy(details = extractDetails(alertsFrameworksDetails.get(af.name)))
            }
          )
        },
        code = code.map { c =>
          c.copy(
            details = extractDetails(codeDetails.get(c.name)),
          )
        }
      )
    }
  }

  private def extractDetails(detailsMap: Option[Value]): Option[Map[String, String]] = {
    import scala.jdk.CollectionConverters._
    detailsMap.map(_.asMap[String](Values.ofString()).asScala.toMap)
  }
}
