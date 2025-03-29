package io.infinitic.storage.keySet

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe

abstract class KeySetStorageTests : StringSpec() {
  val storage: KeySetStorage by lazy { createStorage() }

  abstract fun createStorage(): KeySetStorage

  abstract suspend fun stopServer()

  abstract suspend fun startServer()

  // Helper function to compare ByteArray sets
  private infix fun Set<ByteArray>?.shouldBe(expected: Set<ByteArray>) {
    val actualStr = this?.map { String(it) }?.toSet()?.sorted()
    val expectedStr = expected.map { String(it) }.toSet().sorted()
    actualStr shouldBe expectedStr
  }

  override suspend fun beforeTest(testCase: TestCase) {
    storage.flush()
    storage.add("foo", "bar".toByteArray())
  }

  override suspend fun afterSpec(spec: Spec) {

    stopServer()
    super.afterSpec(spec)
  }

  init {

    "get should return empty set on unknown key" {
      storage.get("unknown") shouldBe emptySet()
    }

    "get should return set with value" {
      storage.get("foo") shouldBe setOf("bar".toByteArray())
    }

    "add on unknown key should create a new set" {
      storage.add("unknown", "42".toByteArray())
      storage.get("unknown") shouldBe setOf("42".toByteArray())
    }

    "add on known key should add to set" {
      storage.add("foo", "42".toByteArray())
      storage.get("foo") shouldBe setOf("42".toByteArray(), "bar".toByteArray())
    }

    "add known value on known key should do nothing" {
      storage.add("foo", "bar".toByteArray())
      storage.get("foo") shouldBe setOf("bar".toByteArray())
    }

    "remove on unknown key should do nothing" {
      storage.remove("unknown", "42".toByteArray())
    }

    "remove unknown value on known key should do nothing" {
      storage.remove("foo", "42".toByteArray())
      storage.get("foo") shouldBe setOf("bar".toByteArray())
    }

    "remove known value on known key should remove from set" {
      storage.remove("foo", "bar".toByteArray())
      storage.get("foo") shouldBe setOf()
    }

    "get on multiple keys" {
      storage.add("foo", "42".toByteArray())
      val gets = storage.get(setOf("foo", "unknown"))
      gets.keys shouldBe setOf("foo", "unknown")
      gets["foo"]!! shouldBe setOf("bar".toByteArray(), "42".toByteArray())
      gets["unknown"] shouldBe setOf()
    }

    "update on multiple keys" {
      storage.update(
          add = mapOf(
              "foo" to setOf("42".toByteArray(), "43".toByteArray()),
              "bar" to setOf("43".toByteArray()),
          ),
          remove = mapOf(
              "foo" to setOf("bar".toByteArray(), "unknown".toByteArray()),
              "unknown" to setOf("42".toByteArray()),
          ),
      )
      storage.get("foo") shouldBe setOf("42".toByteArray(), "43".toByteArray())
      storage.get("bar") shouldBe setOf("43".toByteArray())
    }
  }
}
