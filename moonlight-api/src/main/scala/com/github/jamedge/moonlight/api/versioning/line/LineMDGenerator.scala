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
        s"""## l:${line.name}
           |Property name|Property value
           |-------------|--------------
           |Name|**${line.name}**""" +
           line.owner.map("\n|Owner|" + _).getOrElse("") +
           line.purpose.map("\n|Purpose|" + _).getOrElse("") +
           (line.notes.map(("\n|Notes|", _)).getOrElse(("", List())) match {
                case (c, l) => c + l.map(n => s"_${n}_").mkString(", ")}) +
           s"\n|Inputs|${line.io.flatMap(io => ioCaptions(io.inputs, outputType)).mkString(", ")}" +
           s"\n|Outputs|${line.io.flatMap(io => ioCaptions(io.outputs, outputType)).mkString(", ")}" +
           listCaption(processCaptions(line.processedBy), "Processed by") +
           listCaption(metricsCaptions(line.metrics), "Metrics") +
           listCaption(alertsCaptions(line.alerts), "Alerts") +
           line.code.map(c => s"\n|Code path|[${c.name}](${c.remotePath})").getOrElse("") +
           line.code.map(c => s"\n|Execution command entry point|${executionCommand(c)}").getOrElse("")
      result.stripMargin
    }
  }

  private def listCaption(list: List[String], name: String): String = {
    if (list.isEmpty) {
      ""
    } else {
      s"\n|$name|${list.mkString(", ")}"
    }
  }

  private def ioCaptions(ioElements: List[IOElement], outputType: FormattedOutputType): List[String] = {
    ioElements.map { i =>
      val input = ioLink(i)
      val link = lineageLink(i, outputType)
      s"$input [$link]"
    }
  }

  private def ioLink(ioElement: IOElement): String = {
    ioElement.storage.map { s =>
      s"**${s.name}**: ${ioElement.name}"
    }.getOrElse(defaultIOLink(ioElement))
  }

  private def defaultIOLink(ioElement: IOElement): String = {
    ioElement.name
  }

  private def lineageLink(ioElement: IOElement, outputType: FormattedOutputType): String = {
    outputType match {
      case FormattedOutputType.HTML => lineageLinkHTML(ioElement)
      case FormattedOutputType.Md => lineageLinkMD(ioElement)
    }
  }

  private def lineageLinkHTML(ioElement: IOElement): String = {
    s"[->](/lineage/graph/${ioElement.name})"
  }

  private def lineageLinkMD(ioElement: IOElement): String = {
    s"[->](#lng:${ioElement.name})"
  }

  private def processCaptions(processes: List[Process]): List[String] = {
    processes.map { p =>
      p.processingFramework.map { pf =>
        s"**${pf.name}**: ${p.name}"
      }.getOrElse(p.name)
    }
  }

  private def metricsCaptions(metrics: List[Metric]): List[String] = {
    metrics.map { m =>
      m.metricFramework.map { mf =>
        s"**${mf.name}**: ${m.name}"
      }.getOrElse(m.name)
    }
  }

  private def alertsCaptions(alerts: List[Alert]): List[String] = {
    alerts.map { a =>
      a.alertsFramework.map { af =>
        s"**${af.name}**: ${a.name}"
      }.getOrElse(a.name)
    }
  }

  private def executionCommand(code: Code): String = {
    s"`${code.entryPointClass}${code.entryPointArguments.map(epa => s" ${epa.mkString(" ")}`").getOrElse("`")}"
  }
}
