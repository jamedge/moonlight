package com.github.jamedge.moonlight.core.service.line

import com.github.jamedge.moonlight.core.model.{Alert, AlertsFramework, Code, IO, IOElement, Line, Metric, MetricsFramework, Process, ProcessingFramework, ProcessingHistoryRecord, Storage}
import org.neo4j.driver.v1.Value

object LineBuilder {
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

  def buildLine(rawLineDataRecords: List[RawLineDataRecord]): Line = {
    println(rawLineDataRecords) // TODO: extract data from here and create a proper line
    Line(
      "test_name",
      None,
      None,
      None,
      None,
      List(IO(
        List(IOElement("ti", None, None, None, None, None, None)),
        List(IOElement("to", None, None, None, None, None, None))
      )),
      List(),
      List(),
      List(),
      None
    )
  }
}
