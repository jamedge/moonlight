package com.github.jamedge.moonlight

package object client {
  private val module: Module = new Module("moonlight-core")

  val line: LineClient = module.lineClient
}
