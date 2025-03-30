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

    "putWithVersion null should success on new key" {
      val success = storage.putWithVersion("newKey", null, 0L)
      success shouldBe true

      val (state, version) = storage.getStateAndVersion("newKey")
      state shouldBe null
      version shouldBe 0L
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

    "getStatesAndVersions should return empty map for empty input" {
      storage.getStatesAndVersions(emptySet()) shouldBe emptyMap()
    }

    "getStatesAndVersions should return correct states and versions for multiple keys" {
      // Setup additional test data
      storage.putWithVersion("foo2", "bar2".toByteArray(), 0L)
      storage.putWithVersion("foo3", null, 0L)

      val result = storage.getStatesAndVersions(setOf("foo", "foo2", "foo3", "unknown"))

      // Check foo (existing key with value)
      result["foo"]?.first?.contentEquals("bar".toByteArray()) shouldBe true
      result["foo"]?.second shouldBe 1L

      // Check foo2 (newly added key with value)
      result["foo2"]?.first?.contentEquals("bar2".toByteArray()) shouldBe true
      result["foo2"]?.second shouldBe 1L

      // Check foo3 (deleted key)
      result["foo3"]?.first shouldBe null
      result["foo3"]?.second shouldBe 0L

      // Check unknown key
      result["unknown"]?.first shouldBe null
      result["unknown"]?.second shouldBe 0L
    }

    "putWithVersions version 0 edge cases" {
      // Setup: ensure 'foo' exists (version 1), 'newKey' does not exist.
      storage.get("newKey") shouldBe null
      val (_, fooVersion) = storage.getStateAndVersion("foo")
      fooVersion shouldBe 1L

      val updates = mapOf(
        "foo" to Pair(null, 0L),                   // Fail: Delete existing with version 0
        "newKey" to Pair("value".toByteArray(), 1L) // Fail: Insert with version > 0
      )

      val results = storage.putWithVersions(updates)

      results["foo"] shouldBe false
      results["newKey"] shouldBe false

      // Verify state unchanged
      storage.getStateAndVersion("foo").second shouldBe fooVersion
      storage.get("newKey") shouldBe null

      // Test successful deletion of non-existent key with version 0
      val deleteNonExistentResult = storage.putWithVersions(mapOf("newKey2" to Pair(null, 0L)))
      deleteNonExistentResult["newKey2"] shouldBe true
      storage.get("newKey2") shouldBe null
    }

    "putWithVersions operations on non-existent keys with version > 0" {
      storage.get("nonExistent1") shouldBe null
      storage.get("nonExistent2") shouldBe null

      val updates = mapOf(
        "nonExistent1" to Pair("value".toByteArray(), 1L), // Fail: Update non-existent with version > 0
        "nonExistent2" to Pair(null, 1L)                    // Fail: Delete non-existent with version > 0
      )

      val results = storage.putWithVersions(updates)

      results["nonExistent1"] shouldBe false
      results["nonExistent2"] shouldBe false

      // Verify state unchanged
      storage.get("nonExistent1") shouldBe null
      storage.get("nonExistent2") shouldBe null
    }

    "putWithVersions large batch basic check" {
      val batchSize = 50
      val initialStates = (1..batchSize).map { "key$it" to storage.getStateAndVersion("key$it") }.toMap()
      val updates = initialStates.mapValues { (_, state) ->
        Pair("newValue${state.second + 1}".toByteArray(), state.second)
      }

      val results = storage.putWithVersions(updates)

      // Check if all operations reported success
      results.all { it.value } shouldBe true
      results.size shouldBe batchSize

      // Verify a few keys to ensure state changed correctly
      val finalState1 = storage.getStateAndVersion("key1")
      finalState1.first?.contentEquals("newValue${initialStates["key1"]!!.second + 1}".toByteArray()) shouldBe true
      finalState1.second shouldBe initialStates["key1"]!!.second + 1

      val finalStateLast = storage.getStateAndVersion("key$batchSize")
      finalStateLast.first?.contentEquals("newValue${initialStates["key$batchSize"]!!.second + 1}".toByteArray()) shouldBe true
      finalStateLast.second shouldBe initialStates["key$batchSize"]!!.second + 1
    }

    "putWithVersions small batch (below threshold) should succeed" {
      val batchSize = 5  // Below the 10 threshold
      val initialStates = (1..batchSize).map { "smallKey$it" to storage.getStateAndVersion("smallKey$it") }.toMap()
      val updates = initialStates.mapValues { (_, state) ->
        Pair("smallBatch${state.second + 1}".toByteArray(), state.second)
      }

      val results = storage.putWithVersions(updates)

      // Check if all operations reported success
      results.all { it.value } shouldBe true
      results.size shouldBe batchSize

      // Verify all keys were updated correctly
      initialStates.keys.forEach { key ->
        val (value, version) = storage.getStateAndVersion(key)
        value?.contentEquals("smallBatch${initialStates[key]!!.second + 1}".toByteArray()) shouldBe true
        version shouldBe initialStates[key]!!.second + 1
      }
    }

    "putWithVersions medium batch (around threshold) should succeed" {
      val batchSize = 12  // Just above the 10 threshold
      val initialStates = (1..batchSize).map { "mediumKey$it" to storage.getStateAndVersion("mediumKey$it") }.toMap()
      val updates = initialStates.mapValues { (_, state) ->
        Pair("mediumBatch${state.second + 1}".toByteArray(), state.second)
      }

      val results = storage.putWithVersions(updates)

      // Check if all operations reported success
      results.all { it.value } shouldBe true
      results.size shouldBe batchSize

      // Verify all keys were updated correctly
      initialStates.keys.forEach { key ->
        val (value, version) = storage.getStateAndVersion(key)
        value?.contentEquals("mediumBatch${initialStates[key]!!.second + 1}".toByteArray()) shouldBe true
        version shouldBe initialStates[key]!!.second + 1
      }
    }
  }
}
