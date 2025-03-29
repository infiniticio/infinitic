/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.storage.keyValue

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class KeyValueStorageTests : StringSpec() {
  val storage: KeyValueStorage by lazy { createStorage() }

  abstract fun createStorage(): KeyValueStorage

  abstract suspend fun stopServer()

  abstract suspend fun startServer()

  override suspend fun beforeTest(testCase: TestCase) {
    storage.flush()
    storage.put("foo", "bar".toByteArray())
  }

  override suspend fun beforeSpec(spec: Spec) {
    super.beforeSpec(spec)
    startServer()
  }
  
  override suspend fun afterSpec(spec: Spec) {
    withContext(Dispatchers.IO) {
      storage.close()
    }
    stopServer()
    super.afterSpec(spec)
  }

  init {

    "get should return null on unknown key" {
      storage.get("unknown") shouldBe null
    }

    "get should return value" {
      storage.get("foo").contentEquals("bar".toByteArray()) shouldBe true
    }

    "put on new key should create value" {
      storage.put("foo2", "bar2".toByteArray())
      storage.get("foo2").contentEquals("bar2".toByteArray()) shouldBe true
    }

    "put on existing key should update value" {
      storage.put("foo", "bar2".toByteArray())
      storage.get("foo").contentEquals("bar2".toByteArray()) shouldBe true
    }

    "del on unknown key does nothing" {
      storage.put("unknown", null)
    }

    "del should delete value" {
      storage.put("foo", null)
      storage.get("foo") shouldBe null
    }

    "getSet should return null on unknown key" {
      storage.get(setOf("foo", "unknown")) shouldBe mapOf(
          "foo" to "bar".toByteArray(),
          "unknown" to null,
      )
    }

    "putSet on new key should create value" {
      storage.put(
          mapOf(
              "foo2" to "bar2".toByteArray(),
              "foo3" to "bar3".toByteArray(),
          ),
      )

      storage.get(setOf("foo2", "foo3")) shouldBe mapOf(
          "foo2" to "bar2".toByteArray(),
          "foo3" to "bar3".toByteArray(),
      )
    }

    "putSet on existing key should update value" {
      storage.put(
          mapOf(
              "foo" to "bar2".toByteArray(),
              "foo3" to "bar3".toByteArray(),
          ),
      )

      storage.get(setOf("foo", "foo3")) shouldBe mapOf(
          "foo" to "bar2".toByteArray(),
          "foo3" to "bar3".toByteArray(),
      )
    }

    "delSet on unknown key does nothing" {
      shouldNotThrowAny { storage.put(mapOf("foo" to null, "unknown" to null)) }

      storage.get(setOf("foo", "unknown")) shouldBe mapOf(
          "foo" to null,
          "unknown" to null,
      )
    }

    "getStateAndVersion should return (null, 0) for unknown key" {
      val (state, version) = storage.getStateAndVersion("unknown")
      state shouldBe null
      version shouldBe 0L
    }

    "getStateAndVersion should return correct state and version" {
      val (state, version) = storage.getStateAndVersion("foo")
      state.contentEquals("bar".toByteArray()) shouldBe true
      version shouldBe 1L
    }

    "putWithVersion should succeed with version 0 on new key" {
      val success = storage.putWithVersion("newKey", "value".toByteArray(), 0L)
      success shouldBe true

      val (state, version) = storage.getStateAndVersion("newKey")
      state.contentEquals("value".toByteArray()) shouldBe true
      version shouldBe 1L
    }

    "putWithVersion should fail with wrong version on existing key" {
      val success = storage.putWithVersion("foo", "newValue".toByteArray(), 0L)
      success shouldBe false

      val (state, version) = storage.getStateAndVersion("foo")
      state.contentEquals("bar".toByteArray()) shouldBe true
      version shouldBe 1L
    }

    "putWithVersion should succeed with correct version on existing key" {
      val (_, initialVersion) = storage.getStateAndVersion("foo")
      val success = storage.putWithVersion("foo", "newValue".toByteArray(), initialVersion)
      success shouldBe true

      val (state, version) = storage.getStateAndVersion("foo")
      state.contentEquals("newValue".toByteArray()) shouldBe true
      version shouldBe initialVersion + 1
    }

    "putWithVersion null should delete with correct version" {
      val (_, initialVersion) = storage.getStateAndVersion("foo")
      val success = storage.putWithVersion("foo", null, initialVersion)
      success shouldBe true

      val (state, version) = storage.getStateAndVersion("foo")
      state shouldBe null
      version shouldBe 0L
    }

    "putWithVersion null should fail with wrong version" {
      val success = storage.putWithVersion("foo", null, 0L)
      success shouldBe false

      val (state, version) = storage.getStateAndVersion("foo")
      state.contentEquals("bar".toByteArray()) shouldBe true
      version shouldBe 1L
    }

    "putWithVersion should increment version on each successful update" {
      // Initial state
      val (_, v1) = storage.getStateAndVersion("foo")
      v1 shouldBe 1L

      // First update
      storage.putWithVersion("foo", "value2".toByteArray(), v1) shouldBe true
      val (_, v2) = storage.getStateAndVersion("foo")
      v2 shouldBe 2L

      // Second update
      storage.putWithVersion("foo", "value3".toByteArray(), v2) shouldBe true
      val (_, v3) = storage.getStateAndVersion("foo")
      v3 shouldBe 3L
    }
  }
}
