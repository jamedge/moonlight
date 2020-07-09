package com.github.jamedge.moonlight.core.api.versioning.line

import com.github.jamedge.moonlight.core.model.Line

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
           |notes|${line.notes.getOrElse(List()).map(n => s"_${n}_").mkString(s", ")}
          """.stripMargin
      result
    }
  }
}
