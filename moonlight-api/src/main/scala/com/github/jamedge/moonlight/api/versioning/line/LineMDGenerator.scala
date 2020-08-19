package com.github.jamedge.moonlight.api.versioning.line

import com.github.jamedge.moonlight.api.ApiConfig
import com.github.jamedge.moonlight.api.versioning.FormattedOutputType
import com.github.jamedge.moonlight.core.model.{Alert, Code, IOElement, Line, Metric, Process}

import scala.util.Try

class LineMDGenerator(
    apiConfig: ApiConfig
) {
  def generateMd(line: Line, outputType: FormattedOutputType = FormattedOutputType.Md): Try[String] = {
    Try { // TODO: make other elements output type dependent
      val result =
        s"""## ${line.name}
           |Property name|Property value
           |-------------|--------------
           |Name|**${line.name}**\n""" +
           line.owner.map("|Owner|" + _ + "\n").getOrElse("") +
           line.purpose.map("|Purpose|" + _ + "\n").getOrElse("") +
           (line.notes.map(("|Notes|", _)).getOrElse(("", List())) match {
                case (c, l) => c + l.map(n => s"_${n}_").mkString(", ") + "\n"}) +
           s"|Inputs|${line.io.flatMap(io => generateIOCaptions(io.inputs, outputType)).mkString(", ")}\n" +
           s"|Outputs|${line.io.flatMap(io => generateIOCaptions(io.outputs, outputType)).mkString(", ")}\n" +
           generateListCaption(generateProcessCaptions(line.processedBy), "Processed by") +
           generateListCaption(generateMetricsCaptions(line.metrics), "Metrics") +
           generateListCaption(generateAlertsCaptions(line.alerts), "Alerts") +
           generateListCaption(generateProcessCaptions(line.processedBy), "Processed by") +
           line.code.map(c => s"|Code path|[${c.name}](${c.remotePath})\n").getOrElse("") +
           line.code.map(c => s"|Execution command entry point|${generateExecutionCommand(c)}").getOrElse("")
      result.stripMargin
    }
  }

  private def generateListCaption(list: List[String], name: String): String = {
    if (list.isEmpty) {
      ""
    } else {
      s"|$name|${list.mkString(", ")}\n"
    }
  }

  // TODO: fix all links and anchors based on output type
  private def generateIOCaptions(ioElements: List[IOElement], outputType: FormattedOutputType): List[String] = {
    ioElements.map { i =>
      val inputLink = generateIOLink(i)
      val lineageLink = generateLineageLink(i, outputType)
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

  private def generateLineageLink(ioElement: IOElement, outputType: FormattedOutputType): String = {
    outputType match {
      case FormattedOutputType.HTML => generateLineageLinkHTML(ioElement)
      case FormattedOutputType.Md => generateLineageLinkMD(ioElement)
    }
  }

  private def generateLineageLinkHTML(ioElement: IOElement): String = {
    s"[->](http://${apiConfig.server.host}:${apiConfig.server.port}/lineage/graph/${ioElement.name})"
  }

  private def generateLineageLinkMD(ioElement: IOElement): String = {
    s"[->](#${ioElement.name})"
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
