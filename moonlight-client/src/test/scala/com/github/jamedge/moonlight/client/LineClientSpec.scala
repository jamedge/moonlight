package com.github.jamedge.moonlight.client

import com.github.jamedge.moonlight.core.model.{IO, IOElement, Line}
import com.github.jamedge.moonlight.core.service.line.{LinePersistenceLayer, LineService}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.{reset, verify, times}
import org.scalatest.BeforeAndAfterEach

class LineClientSpec extends AnyFunSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
  val module = new Module("moonlight-client") {
    override lazy val lineService: LineService = mock[LineService]
  }

  override def afterEach(): Unit = {
    super.afterEach()
    reset(module.lineService)
  }

  val subject: LineClient = module.lineClient

  implicit val testLine: Line = Line(
    name = "test_line",
    owner = Some("test_owner"),
    purpose = Some("test_purpose"),
    notes = Some(List("test_note1", "test_note_2")),
    details = None,
    io = List(IO(
      List(IOElement("test_input", None, None, None, None, None, None)),
      List(IOElement("test_output", None, None, None, None, None, None))
    )),
    processedBy = List(),
    metrics = List(),
    alerts = List(),
    code = None
  )

  describe("#persist") {
    it ("should persist the line correctly") {
      subject.persist(testLine)

      verify(module.lineService, times(1)).addLine(testLine)
    }
  }
}
