package com.github.jamedge.moonlight.core.service.fixture

/**
 * Fixture abstract class intended for creation of pool of
 * test object.
 *
 * @tparam T Type of the test object.
 */
abstract class Fixture[T] {
  def prefixShort: String
  def prefixLong: String
  def pool: Map[String, T]

  /**
   * Creates an object name based on index and long prefix.
   * @param index Index value used to create a name.
   * @return Indexed object name.
   */
  def name(index: Int): String = {
    s"${prefixLong}_$index"
  }

  /**
   * Creates an object pool key based on index and short prefix.
   * @param index Index value used to create a key.
   * @return Indexed pool key.
   */
  def key(index: Int): String = {
    s"$prefixShort$index"
  }

  /**
   * Gets the object from pool based on the index.
   * @param index Index based on which object is fetched from the pool.
   * @return Object fetched from the pool based on the index value.
   */
  def get(index: Int): Option[T] = {
    pool.get(key(index))
  }
}

/**
 * Fixture abstract class intended for creation of pool of
 * test object based on defined index bounds.
 * @param from Starting index bound.
 * @param to Starting index bound.
 * @tparam T Type of the test object.
 */
abstract class FixtureBounded[T](from: Int, to: Int) extends Fixture[T] {
  /**
   * Creates an empty object with the specified index key.
   * @param index Index value used for naming.
   * @return
   */
  def emptyObject(index: Int): T

  /**
   * Creates pool of indexed objects in the form of Map for the provided bounds.
   * Returns empty map if end bound is less than starting bound.
   * @param from Starting index bound.
   * @param to Ending index bound.
   * @return Pool of indexed objects.
   */
  def createPool(from: Int, to: Int): Map[String, T] = {
    if (from <= to) {
      (from to to).map { index =>
        key(index) -> emptyObject(index)
      }.toMap
    } else {
      Map()
    }
  }

  override def pool: Map[String, T] = createPool(from, to)
}
