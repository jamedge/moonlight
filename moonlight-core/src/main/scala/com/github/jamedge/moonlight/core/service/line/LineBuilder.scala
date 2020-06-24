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
      processedByDetails: List[(Process, Value)]
  ): Line = {
    line.map{ l =>
      l.copy(
        details = extractDetails(lineDetails),
        processedBy = processedBy.map { pb =>
          pb.copy(details = extractDetails(processedByDetails.filter(_._1.name == pb.name).map(_._2).headOption))
        }
      )
    }.getOrElse(Line("", None, None, None, None, List(), List(), List(), List(), None))
  }

  private def extractDetails(detailsMap: Option[Value]): Option[Map[String, String]] = {
    import scala.jdk.CollectionConverters._
    detailsMap.map(_.asMap[String](Values.ofString()).asScala.toMap)
  }
}
