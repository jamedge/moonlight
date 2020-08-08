package com.github.jamedge.moonlight.core.api.versioning

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import com.github.jamedge.moonlight.core.api.versioning.line.LineMDGenerator

import scala.concurrent.ExecutionContext

trait HTMLSupport[T <: Object] {
  /**
   * Base marshaller for the object of class <T>. Override it before usage.
   * @param executionContext Implicit execution context.
   * @param lineMDGenerator Generator that generates markdown out of line.
   * @param lineHTMLGenerator Generator that generates html out of markdown.
   * @return Resulting marshaller.
   */
  implicit def marshaller(
      implicit executionContext: ExecutionContext,
      lineMDGenerator: LineMDGenerator,
      lineHTMLGenerator: HTMLGenerator
  ): ToResponseMarshaller[T]
}
