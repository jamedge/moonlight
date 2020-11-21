package com.github.jamedge.moonlight.api.versioning.report

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import com.github.jamedge.moonlight.api.versioning.line.LineHTMLSupport.generateLineHTML
import com.github.jamedge.moonlight.api.versioning.{FormattedOutputType, HTMLGenerator}
import com.github.jamedge.moonlight.api.versioning.line.LineMDGenerator
import com.github.jamedge.moonlight.api.versioning.lineage.GraphFormatter
import com.github.jamedge.moonlight.core.service.line.LineService
import com.github.jamedge.moonlight.core.service.lineage.LineageService

import scala.concurrent.{ExecutionContext, Future}

class ReportGenerator(
    lineService: LineService,
    lineageService: LineageService,
    graphFormatter: GraphFormatter
)(
    implicit val executionContext: ExecutionContext,
    actorSystem: ActorSystem,
    lineMDGenerator: LineMDGenerator,
    htmlGenerator: HTMLGenerator
) {

  def getLinesHtml(): Future[String] = {
    for {
      lines <- lineService.getLines
      bodyString <- Future(lines.map(line => generateLineHTML(line)).mkString("<br>"))
      headerString <- Future(generateHeaderHtml)
      footerString <- Future(generateFooterHtml)
      htmlString <- Future(headerString + bodyString + footerString) // TODO: add table of contents
    } yield htmlString
  }

  def getLinesMarkdown(): Future[String] = {
    for {
      lines <- lineService.getLines
      linesString <- Future(lines.map(line =>
        lineMDGenerator.
          generateMd(line).
          getOrElse(throw LineMDGenerationException("Markdown generation failed!"))).mkString("\n\n<br>\n\n"))
      lineageGraphs <- lineageService.getLineageGraphs
      lineagesString <- Future("\n<br>\n\n# Lineage\n" + lineageGraphs.map(graph => graphFormatter.
        formatLineageGraph(graph, FormattedOutputType.Md)).mkString("\n\n<br>\n\n"))
      headerString <- Future(generateHeaderHtml)
      footerString <- Future(generateFooterHtml)
      htmlString <- Future(
        headerString + "\n" +
          linesString + "\n" +
          lineagesString + "\n<hr>\n" +
          footerString) // TODO: add table of contents
    } yield htmlString
  }

  private def generateHeaderHtml: String = {
    """
      |<h1>Lines report</h1>
      |<p>The list of all lines entered to the database is presented below.</p>
    """.stripMargin
  }

  private def generateFooterHtml: String = {
    s"<p>Generated by `moonlight` on ${ZonedDateTime.now()}.</p>"
  }
}

case class LineMDGenerationException(message: String) extends Exception(message)
