package com.github.jamedge.moonlight.core.service.line

import com.github.jamedge.moonlight.core.model.{Alert, AlertsFramework, Code, IO, IOElement, Line, Metadata, Metric, MetricsFramework, Process, ProcessingFramework, ProcessingHistoryRecord, Storage}
import org.neo4j.driver.v1.{Value, Values}

object LineBuilder {

  // TODO: Depricate these classes as soon as proper line building is completed
  case class ProcessingHistoryRecordLight(triggeredAt: String, triggeredBy: String)
  case class RawLineDataRecord(
      line: Line,								                              // line,
      lineDetails: Value, 					                          // line details
      input: IOElement,						                            // input
      inputDetails: Value,					                          // input details
      inputStorage: Storage,					                        // input storage
      inputStorageDetails: Value,			  	                    // input storage details
      output: IOElement,					 	                          // output
      outputDetails: Value,					                          // output details
      outputStorage: Storage,					                        // output storage
      outputStorageDetails: Value,		   	                    // output storage details
      processedBy: Process,					                          // processedBy
      processedByDetails: Value,  			                      // processedBy details
      processingFramework: ProcessingFramework,               // processing framework
      processingFrameworkDetails: Value,		                  // processing framework details
      processingHistoryRecord: ProcessingHistoryRecordLight,	// triggered
      metric: Metric,							                            // metric
      metricDetails: Value,					                          // metric details
      metricFramework: MetricsFramework,  		                // metric framework
      metricFrameworkDetails: Value,			                    // metric framework details
      alert: Alert,							                              // alert
      alertDetails: Value,					                          // alert details
      alertFramework: AlertsFramework,		                    // alert framework
      alertFrameworkDetails: Value,			                      // alert framework details
      code: Code,								                              // code
      codeDetails: Value                     	                // code details
  )

  // TODO: build up this function to create the whole line
  def buildLine(
      line: Option[Line],
      lineDetails: Option[Value],
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
  ): Line = {
    line.map{ l =>
      l.copy(
        details = extractDetails(lineDetails),
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
    }.getOrElse(Line("", None, None, None, None, List(), List(), List(), List(), None))
  }

  private def extractDetails(detailsMap: Option[Value]): Option[Map[String, String]] = {
    import scala.jdk.CollectionConverters._
    detailsMap.map(_.asMap[String](Values.ofString()).asScala.toMap)
  }
}
