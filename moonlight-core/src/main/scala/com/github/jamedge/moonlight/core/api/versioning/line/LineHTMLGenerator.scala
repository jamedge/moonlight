package com.github.jamedge.moonlight.core.api.versioning.line

import scala.util.Try
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.util.data.MutableDataSet

class LineHTMLGenerator {
  def generateHTML(lineMD: String): Try[String] = Try {
    val options = new MutableDataSet

    options.set(Parser.EXTENSIONS, java.util.Arrays.asList(TablesExtension.create, StrikethroughExtension.create))
    options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, new java.lang.Boolean(true))
    options.set(AnchorLinkExtension.ANCHORLINKS_SET_ID, new java.lang.Boolean(true))
    options.set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, new java.lang.String("anchor"))
    options.set(AnchorLinkExtension.ANCHORLINKS_SET_NAME, new java.lang.Boolean(true))

    val parser = Parser.builder(options).build
    val renderer = HtmlRenderer.builder(options).build

    val document = parser.parse(lineMD)
    renderer.render(document)
  }
}
