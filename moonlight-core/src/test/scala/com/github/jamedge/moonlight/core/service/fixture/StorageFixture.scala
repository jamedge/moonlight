package com.github.jamedge.moonlight.core.service.fixture

import com.github.jamedge.moonlight.core.model.Storage

/**
 * Fixture for Storage class.
 */
class StorageFixture(from: Int, to: Int) extends FixtureBounded[Storage](from, to) {
  override def prefixShort: String = "ts"
  override def prefixLong: String = "test_storage"

  override def emptyObject(index: Int): Storage = {
    Storage(name(index), None, None, None, None, None)
  }
}
