package com.github.jamedge.moonlight.api.handlers

import akka.http.scaladsl.model.StatusCode

class ApiException(
    val originalException: Throwable,
    val responseStatusCode: StatusCode,
    responseMessageOverride: Option[String] = None
) extends Exception(originalException) {
  def responseMessage(): String = {
    responseMessageOverride.getOrElse(responseStatusCode.defaultMessage())
  }
}
