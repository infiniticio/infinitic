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
      val numThreads = 100

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

      val numThreads = 100
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

      val numThreads = 100
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
      val numThreads = 100

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
  }
}
