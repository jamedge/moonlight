package com.github.jamedge.moonlight.api.versioning

import com.github.jamedge.moonlight.api.Module
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Success

class HTMLGeneratorSpec extends AnyFunSpec with Matchers {
  val module = new Module("moonlight-api")
  val subject = module.htmlGenerator

  val testMd: String =
    """
      |## test-line
      |Property name|Property value
      |-------------|--------------
      |Name|test-name\n
    """.stripMargin

  val testHtml: String =
    """
      |<h2><a href="#test-line" id="test-line" name="test-line" class="anchor">test-line</a></h2>
      |<table>
      |<thead>
      |<tr><th>Property name</th><th>Property value</th></tr>
      |</thead>
      |<tbody>
      |<tr><td>Name</td><td>test-name\n</td></tr>
      |</tbody>
      |</table>
    """.stripMargin

  describe("#generateMd") {
    it ("should generate the html correctly") {
      val Success(generatedHtml) = subject.generateHTML(testMd)

      generatedHtml.trim shouldBe testHtml.trim
    }
  }
}
