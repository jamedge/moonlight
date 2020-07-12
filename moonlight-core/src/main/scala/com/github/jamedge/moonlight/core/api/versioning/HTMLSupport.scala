package com.github.jamedge.moonlight.core.api.versioning

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import com.github.jamedge.moonlight.core.api.versioning.line.{LineHTMLGenerator, LineMDGenerator}

import scala.concurrent.ExecutionContext

trait HTMLSupport[T <: Object] {
  /**
   * Base marshaller for the object of class <T>. Override it before usage.
   * @param executionContext Implicit execution context.
   * @return Resulting marshaller.
   */
  implicit def marshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: LineHTMLGenerator
  ): ToResponseMarshaller[T]
}
