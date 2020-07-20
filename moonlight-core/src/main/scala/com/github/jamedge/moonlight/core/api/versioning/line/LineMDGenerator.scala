package com.github.jamedge.moonlight.core.api.versioning.line

import com.github.jamedge.moonlight.core.api.ApiConfig
import com.github.jamedge.moonlight.core.model.{Alert, Code, IOElement, Line, Metric, Process}

import scala.util.Try

class LineMDGenerator(
    apiConfig: ApiConfig
) {
  def generateMd(line: LineV1): Try[String] = {
    Try {
      val naString = "<NA>"
      val result =
        s"""Property name|Property value
           |-------------|--------------
           |Name|**${line.name}**
           |Owner|${line.owner.getOrElse(naString)}
           |Purpose|${line.purpose.getOrElse(naString)}
           |Notes|${line.notes.getOrElse(List()).map(n => s"_${n}_").mkString(", ")}
           |Inputs|${line.io.flatMap(io => generateIOCaptions(io.inputs)).mkString(", ")}
           |Outputs|${line.io.flatMap(io => generateIOCaptions(io.outputs)).mkString(", ")}
           |Processed by|${generateProcessCaptions(line.processedBy).mkString(", ")}
           |Metrics|${generateMetricsCaptions(line.metrics).mkString(", ")}
           |Alerts|${generateAlertsCaptions(line.alerts).mkString(", ")}
           |Code path|${line.code.map(c => s"[${c.name}](${c.remotePath})").getOrElse(naString)}
           |Execution command entry point|${line.code.map(c => generateExecutionCommand(c)).getOrElse(naString)}
          """.stripMargin
      result
    }
  }

  private def generateIOCaptions(ioElements: List[IOElement]): List[String] = {
    ioElements.map { i =>
      val inputLink = generateIOLink(i)
      val lineageLink = generateLineageLink(i)
      s"$inputLink [$lineageLink]"
    }
  }

  private def generateIOLink(ioElement: IOElement): String = {
    ioElement.storage.map { s =>
      s"[**${s.name}**: ${ioElement.name}]" +
        s"${s.locationPath.map(lp => s"(#$lp${if (lp.endsWith("/")) "" else "/"}").getOrElse("(#")}" +
        s"${ioElement.locationRelativePath.map(rp => s"$rp)").getOrElse(s"${ioElement.name})")}"
    }.getOrElse(generateDefaultIOLink(ioElement))
  }

  private def generateDefaultIOLink(ioElement: IOElement): String = {
    s"[${ioElement.name}](#${ioElement.locationRelativePath.getOrElse(ioElement.name)})"
  }

  private def generateLineageLink(ioElement: IOElement): String = {
    s"[->](http://${apiConfig.server.host}:${apiConfig.server.port}/lineage/graph?root_io=${ioElement.name})"
  }

  private def generateProcessCaptions(processes: List[Process]): List[String] = {
    processes.map { p =>
      p.processingFramework.map { pf =>
        s"[**${pf.name}**: ${p.name}]" +
          s"${pf.locationPath.map(fp => s"(#$fp${if (fp.endsWith("/")) "" else "/"}").getOrElse("(#")}" +
          s"${p.locationRelativePath.map(rp => s"$rp)").getOrElse(s"${p.name})")}"
      }.getOrElse(s"[${p.name}](#${p.locationRelativePath.getOrElse(p.name)})")
    }
  }

  private def generateMetricsCaptions(metrics: List[Metric]): List[String] = {
    metrics.map { m =>
      m.metricFramework.map { mf =>
        s"[**${mf.name}**: ${m.name}]" +
          s"${mf.locationPath.map(fp => s"(#$fp${if (fp.endsWith("/")) "" else "/"}").getOrElse("(#")}" +
          s"${m.locationRelativePath.map(rp => s"$rp)").getOrElse(s"${m.name})")}"
      }.getOrElse(s"[${m.name}](#${m.locationRelativePath.getOrElse(m.name)})")
    }
  }

  private def generateAlertsCaptions(alerts: List[Alert]): List[String] = {
    alerts.map { a =>
      a.alertsFramework.map { af =>
        s"[**${af.name}**: ${a.name}]" +
          s"${af.locationPath.map(fp => s"(#$fp${if (fp.endsWith("/")) "" else "/"}").getOrElse("(#")}" +
          s"${a.locationRelativePath.map(rp => s"$rp)").getOrElse(s"${a.name})")}"
      }.getOrElse(s"[${a.name}](#${a.locationRelativePath.getOrElse(a.name)})")
    }
  }

  private def generateExecutionCommand(code: Code): String = {
    s"`${code.entryPointClass}${code.entryPointArguments.map(epa => s" ${epa.mkString(" ")}`").getOrElse("`")}"
  }
}
