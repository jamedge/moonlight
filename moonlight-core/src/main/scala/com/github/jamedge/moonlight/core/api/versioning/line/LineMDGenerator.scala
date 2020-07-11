package com.github.jamedge.moonlight.core.api.versioning.line

import com.github.jamedge.moonlight.core.model.{IOElement, Line}

import scala.util.Try

class LineMDGenerator {
  def generateMd(line: Line): Try[String] = {
    Try {
      val naString = "<NA>"
      val result =
        s"""Property name|Property value
           |-------------|--------------
           |name|${line.name}
           |owner|${line.owner.getOrElse(naString)}
           |purpose|${line.purpose.getOrElse(naString)}
           |notes|${line.notes.getOrElse(List()).map(n => s"_${n}_").mkString(", ")}
           |inputs|${line.io.flatMap(io => generateIOCaptions(io.inputs)).mkString(", ")}
           |outputs|${line.io.flatMap(io => generateIOCaptions(io.outputs)).mkString(", ")}
          """.stripMargin
      result
    }
  }

  private def generateIOCaptions(ioElements: List[IOElement]): List[String] = {
    ioElements.map { i =>
      i.storage.map { s =>
        s"[**${s.name}**: ${i.name}]" +
          s"${s.locationPath.map(slp => s"($slp${if (slp.endsWith("/")) "" else "/"}").getOrElse("(#")}" +
          s"${i.locationRelativePath.map(ilp => s"$ilp)").getOrElse(s"${i.name})")}"
      }.getOrElse("")
    }
  }
}
