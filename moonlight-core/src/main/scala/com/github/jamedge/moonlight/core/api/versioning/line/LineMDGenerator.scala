package com.github.jamedge.moonlight.core.api.versioning.line

import com.github.jamedge.moonlight.core.model.Line

import scala.util.{Success, Try}

class LineMDGenerator {
  def generateMd(line: Line): Try[String] = {
    Success(line.toString) //TODO: implement
  }
}
