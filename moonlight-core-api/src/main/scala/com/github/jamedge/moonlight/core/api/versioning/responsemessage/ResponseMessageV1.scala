package com.github.jamedge.moonlight.core.api.versioning.responsemessage

import com.github.jamedge.moonlight.core.api.routes.ResponseMessage

case class ResponseMessageV1(message: String)

object ResponseMessageV1 {
  def toResponseMessageV1(responseMessage: ResponseMessage): ResponseMessageV1 = {
    ResponseMessageV1(responseMessage.message)
  }

  def fromResponseMessageV1(responseMessageV1: ResponseMessageV1): ResponseMessage = {
    ResponseMessage(responseMessageV1.message)
  }
}
