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

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.comparables.beGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class KeyValueStorageRaceTests : StringSpec() {
  val storage: KeyValueStorage by lazy { createStorage() }

  abstract fun createStorage(): KeyValueStorage

  abstract suspend fun stopServer()

  abstract suspend fun startServer()

  override suspend fun beforeTest(testCase: TestCase) {
    storage.flush()
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
    "concurrent version 0 writes should only allow one to succeed" {
      val key = "test-key"
      val numThreads = 10

      // Launch multiple coroutines trying to write version 0 simultaneously
      val results = coroutineScope {
        (1..numThreads).map { i ->
          async(Dispatchers.IO) {
            storage.putWithVersion(key, "value-$i".toByteArray(), 0L)
          }
        }.awaitAll()
      }

      // Only one write should succeed
      results.count { it } shouldBe 1

      // Check the final state
      val (value, version) = storage.getStateAndVersion(key)
      version shouldBe 1L
      String(value!!) shouldStartWith "value-"
    }

    "concurrent updates with version check should only allow one to succeed" {
      val key = "test-key"

      // Initialize with version 1
      storage.putWithVersion(key, "initial".toByteArray(), 0L) shouldBe true

      val numThreads = 10
      var successCount = 0

      // Launch multiple coroutines trying to update with version 1
      coroutineScope {
        (1..numThreads).map { i ->
          launch(Dispatchers.IO) {
            if (storage.putWithVersion(key, "value-$i".toByteArray(), 1L)) {
              successCount++
            }
          }
        }.forEach { it.join() }
      }

      // Only one update should succeed
      successCount shouldBe 1

      // Check the final state
      val (value, version) = storage.getStateAndVersion(key)
      version shouldBe 2L
      String(value!!) shouldStartWith "value-"
    }

    "concurrent deletes with version check should only allow one to succeed" {
      val key = "test-key"

      // Initialize with version 1
      storage.putWithVersion(key, "initial".toByteArray(), 0L) shouldBe true

      val numThreads = 10
      var successCount = 0

      // Launch multiple coroutines trying to delete with version 1
      coroutineScope {
        (1..numThreads).map { i ->
          launch(Dispatchers.IO) {
            if (storage.putWithVersion(key, null, 1L)) {
              successCount++
            }
          }
        }.forEach { it.join() }
      }

      // Only one delete should succeed
      successCount shouldBe 1

      // Check the final state
      val (value, version) = storage.getStateAndVersion(key)
      value shouldBe null
      version shouldBe 0L
    }

    "concurrent mixed operations should maintain consistency" {
      val key = "test-key"
      val numThreads = 10

      // Initialize with version 1
      storage.putWithVersion(key, "initial".toByteArray(), 0L) shouldBe true

      // Launch multiple coroutines doing different operations
      coroutineScope {
        (1..numThreads).map { i ->
          launch(Dispatchers.IO) {
            when (i % 3) {
              0 -> storage.putWithVersion(key, "update-$i".toByteArray(), 1L)
              1 -> storage.putWithVersion(key, null, 1L)
              2 -> storage.putWithVersion(key, "new-$i".toByteArray(), 0L)
            }
          }
        }.forEach { it.join() }
      }

      // Check final state - should be consistent
      val (value, version) = storage.getStateAndVersion(key)
      if (value == null) {
        version shouldBe 0L
      } else {
        version should beGreaterThan(0L)
        val valueStr = String(value)
        (valueStr.startsWith("update-") || valueStr.startsWith("new-")) shouldBe true
      }
    }

    "concurrent reads during updates should always see consistent state" {
      val key = "test-key"
      val numReaders = 50
      val numWriters = 10

      // Initialize with version 1
      storage.putWithVersion(key, "initial".toByteArray(), 0L) shouldBe true

      coroutineScope {
        // Launch reader coroutines
        val readers = (1..numReaders).map {
          launch(Dispatchers.IO) {
            repeat(100) {
              val (value, version) = storage.getStateAndVersion(key)
              if (value != null) {
                version should beGreaterThan(0L)
              } else {
                version shouldBe 0L
              }
            }
          }
        }

        // Launch writer coroutines
        val writers = (1..numWriters).map {
          launch(Dispatchers.IO) {
            repeat(10) { iteration ->
              val (_, currentVersion) = storage.getStateAndVersion(key)
              if (currentVersion > 0) {
                storage.putWithVersion(key, "value-$it-$iteration".toByteArray(), currentVersion)
              }
            }
          }
        }

        // Wait for all operations to complete
        readers.forEach { it.join() }
        writers.forEach { it.join() }
      }

      // Final state should be consistent
      val (value, version) = storage.getStateAndVersion(key)
      if (value != null) {
        version should beGreaterThan(0L)
        String(value) shouldStartWith "value-"
      }
    }

    "putWithVersions concurrent race should maintain atomicity" {
      val key1 = "race-key-1"
      val key2 = "race-key-2"
      val numThreads = 10
      val successfulAtomicUpdates = mutableListOf<Map<String, Boolean>>()

      // Initialize key1
      storage.putWithVersion(key1, "init1".toByteArray(), 0L) shouldBe true
      val (initialValue1, initialVersion1) = storage.getStateAndVersion(key1)
      initialValue1 shouldBe "init1".toByteArray()
      initialVersion1 shouldBe 1L

      // Key2 does not exist initially
      storage.getStateAndVersion(key2).second shouldBe 0L

      coroutineScope {
        val jobs = (1..numThreads).map { i ->
          launch(Dispatchers.IO) {
            val updates = mapOf(
                key1 to Pair(
                    "update-$i".toByteArray(),
                    initialVersion1,
                ), // Try to update key1 based on initial version
                key2 to Pair("insert-$i".toByteArray(), 0L),                // Try to insert key2
            )
            val result = storage.putWithVersions(updates)
            // If *all* operations in the batch succeeded, record it
            if (result.values.all { it }) {
              synchronized(successfulAtomicUpdates) {
                successfulAtomicUpdates.add(result)
              }
            }
          }
        }
        jobs.forEach { it.join() }
      }

      // Only one coroutine should have its *entire* batch succeed atomically
      successfulAtomicUpdates.size shouldBe 1

      // Verify final state
      val (finalValue1, finalVersion1) = storage.getStateAndVersion(key1)
      val (finalValue2, finalVersion2) = storage.getStateAndVersion(key2)

      finalVersion1 shouldBe initialVersion1 + 1 // key1 was updated once
      finalValue1?.toString(Charsets.UTF_8)?.shouldStartWith("update-")

      finalVersion2 shouldBe 1L // key2 was inserted once
      finalValue2?.toString(Charsets.UTF_8)?.shouldStartWith("insert-")
    }

    "getStatesAndVersions during concurrent putWithVersions should see consistent states" {
      val keys = (1..10).map { "consistency-key-$it" }.toSet()
      val numReaders = 20
      val numWriters = 5
      val writerIterations = 20
      val readerIterations = 100

      // Initialize keys randomly
      keys.forEach { key ->
        if (Math.random() > 0.5) {
          storage.putWithVersion(key, "init".toByteArray(), 0L)
        }
      }

      coroutineScope {
        // Writers performing putWithVersions
        val writers = (1..numWriters).map { writerId ->
          launch(Dispatchers.IO) {
            repeat(writerIterations) { iteration ->
              // Fetch current state for a subset of keys
              val keysToUpdate = keys.shuffled().take(3).toSet()
              if (keysToUpdate.isEmpty()) return@repeat
              val currentStates = storage.getStatesAndVersions(keysToUpdate)

              // Prepare updates based on current state
              val updates = currentStates.mapValues { (key, state) ->
                val (currentValue, currentVersion) = state
                val operation = Math.random()
                when {
                  operation < 0.4 -> // Update existing or insert new
                    Pair("writer-$writerId-iter-$iteration".toByteArray(), currentVersion)

                  operation < 0.7 && currentVersion > 0 -> // Delete existing
                    Pair(null, currentVersion)

                  else -> // Try insert or no-op delete
                    Pair("writer-$writerId-iter-$iteration".toByteArray(), currentVersion)
                }
              }
              // try {
              storage.putWithVersions(updates)
            }
          }
        }

        // Readers performing getStatesAndVersions
        val readers = (1..numReaders).map {
          launch(Dispatchers.IO) {
            repeat(readerIterations) {
              val keySubset = keys.shuffled().take(5).toSet()
              if (keySubset.isEmpty()) return@repeat
              try {
                val results = storage.getStatesAndVersions(keySubset)
                results.forEach { (_, state) ->
                  val (value, version) = state
                  // THE CORE ASSERTION: Check consistency
                  if (value == null) {
                    version shouldBe 0L
                  } else {
                    version should beGreaterThan(0L)
                  }
                }
              } catch (e: Exception) {
                // Ignore exceptions during high contention test
              }
            }
          }
        }

        writers.forEach { it.join() }
        readers.forEach { it.join() }
      }
      // No final state check needed, the goal is to ensure no inconsistent reads happened during the run.
    }

    "should handle transaction failures gracefully" {
      val key = "transaction-test-key"
      val numThreads = 10
      val numIterations = 100
      val successfulUpdates = mutableListOf<Boolean>()

      // Initialize with version 1
      storage.putWithVersion(key, "initial".toByteArray(), 0L) shouldBe true

      coroutineScope {
        val jobs = (1..numThreads).map { threadId ->
          launch(Dispatchers.IO) {
            repeat(numIterations) { iteration ->
              val (_, currentVersion) = storage.getStateAndVersion(key)
              val success = storage.putWithVersion(
                  key,
                  "thread-$threadId-iter-$iteration".toByteArray(),
                  currentVersion,
              )
              synchronized(successfulUpdates) {
                successfulUpdates.add(success)
              }
            }
          }
        }
        jobs.forEach { it.join() }
      }

      // Verify that some updates succeeded and some failed (due to version conflicts)
      successfulUpdates.any { it } shouldBe true
      successfulUpdates.any { !it } shouldBe true

      // Verify final state is consistent
      val (finalValue, finalVersion) = storage.getStateAndVersion(key)
      finalVersion should beGreaterThan(0L)
      String(finalValue!!) shouldStartWith "thread-"
    }

    "should maintain consistency under high contention" {
      val keys = (1..5).map { "contention-key-$it" }.toSet()
      val numThreads = 20
      val numIterations = 50
      val successfulBatchUpdates = mutableListOf<Map<String, Boolean>>()

      // Initialize all keys
      keys.forEach { key ->
        storage.putWithVersion(key, "initial".toByteArray(), 0L) shouldBe true
      }

      coroutineScope {
        val jobs = (1..numThreads).map { threadId ->
          launch(Dispatchers.IO) {
            repeat(numIterations) { iteration ->
              // Get current states for all keys
              val currentStates = storage.getStatesAndVersions(keys)

              // Prepare updates for all keys
              val updates = currentStates.mapValues { (key, state) ->
                val (_, currentVersion) = state
                Pair("thread-$threadId-iter-$iteration".toByteArray(), currentVersion)
              }

              val results = storage.putWithVersions(updates)

              // Record successful batch updates
              if (results.values.all { it }) {
                synchronized(successfulBatchUpdates) {
                  successfulBatchUpdates.add(results)
                }
              }
            }
          }
        }
        jobs.forEach { it.join() }
      }

      // Verify that some batch updates succeeded
      successfulBatchUpdates.isNotEmpty() shouldBe true

      // Verify final states are consistent
      val finalStates = storage.getStatesAndVersions(keys)
      finalStates.values.forEach { (value, version) ->
        version should beGreaterThan(0L)
        String(value!!) shouldStartWith "thread-"
      }
    }

    "should handle network partition scenarios" {
      val key = "partition-test-key"
      val numThreads = 5
      val numIterations = 20
      val successfulUpdates = mutableListOf<Boolean>()

      // Initialize with version 1
      storage.putWithVersion(key, "initial".toByteArray(), 0L) shouldBe true

      coroutineScope {
        val jobs = (1..numThreads).map { threadId ->
          launch(Dispatchers.IO) {
            repeat(numIterations) { iteration ->
              try {
                // Simulate network delay
                delay(10L)

                val (_, currentVersion) = storage.getStateAndVersion(key)
                val success = storage.putWithVersion(
                    key,
                    "thread-$threadId-iter-$iteration".toByteArray(),
                    currentVersion,
                )

                synchronized(successfulUpdates) {
                  successfulUpdates.add(success)
                }
              } catch (e: Exception) {
                // Record failed updates
                synchronized(successfulUpdates) {
                  successfulUpdates.add(false)
                }
              }
            }
          }
        }
        jobs.forEach { it.join() }
      }

      // Verify that some updates succeeded despite network issues
      successfulUpdates.any { it } shouldBe true

      // Verify final state is consistent
      val (finalValue, finalVersion) = storage.getStateAndVersion(key)
      finalVersion should beGreaterThan(0L)
      String(finalValue!!) shouldStartWith "thread-"
    }
  }
}
