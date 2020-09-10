package com.github.jamedge.moonlight.core.service.fixture

import com.github.jamedge.moonlight.core.model.IOElement

/**
 * Fixture for IOElement class.
 */
class IOElementFixture(from: Int, to: Int) extends FixtureBounded[IOElement](from, to) {
  override def prefixShort: String = "tioe"
  override def prefixLong: String = "test_io_element"

  val storageFixture: StorageFixture = new StorageFixture(from, to)

  override def emptyObject(index: Int): IOElement = {
    IOElement(name(index), None, None, None, None, storageFixture.get(index), None)
  }
}
