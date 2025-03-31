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
package io.infinitic.storage.databases.inMemory

import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking

internal data class VersionedValue(
  val value: ByteArray,
  val version: Long
)

internal class InMemoryKeyValueStorage(
  val storage: ConcurrentHashMap<String, AtomicReference<VersionedValue>> = ConcurrentHashMap()
) : KeyValueStorage {
  // Mutex for atomic multi-key operations
  private val mutex = Mutex()

  companion object {
    fun from(config: InMemoryConfig): InMemoryKeyValueStorage {
      val storage = ConcurrentHashMap<String, AtomicReference<VersionedValue>>()
      // Initialize from existing data if any
      config.getPool().keyValue.forEach { (key, value) ->
        storage[key] =
            AtomicReference(VersionedValue(value, config.getPool().keyValueVersions[key] ?: 1L))
      }
      return InMemoryKeyValueStorage(storage)
    }
  }

  override suspend fun get(key: String): ByteArray? =
      storage[key]?.get()?.value

  override suspend fun get(keys: Set<String>): Map<String, ByteArray?> =
      keys.associateWith { get(it) }

  override suspend fun put(key: String, bytes: ByteArray?) {
    when (bytes) {
      null -> storage.remove(key)
      else -> {
        val currentRef = storage[key]
        if (currentRef == null) {
          // New key - start at version 1
          storage[key] = AtomicReference(VersionedValue(bytes, 1L))
        } else {
          // Existing key - increment version
          while (true) {
            val current = currentRef.get()
            if (currentRef.compareAndSet(current, VersionedValue(bytes, current.version + 1L))) {
              break
            }
            // If CAS failed, loop will retry with new current value
          }
        }
      }
    }
  }

  override suspend fun put(bytes: Map<String, ByteArray?>) {
    bytes.forEach { (k, v) -> put(k, v) }
  }

  override fun close() {
    // Do nothing
  }

  override suspend fun getStateAndVersion(key: String): Pair<ByteArray?, Long> {
    val versionedValue = storage[key]?.get()
    return if (versionedValue != null) {
      Pair(versionedValue.value, versionedValue.version)
    } else {
      Pair(null, 0L)
    }
  }

  override suspend fun getStatesAndVersions(keys: Set<String>): Map<String, Pair<ByteArray?, Long>> =
      keys.associateWith { getStateAndVersion(it) }

  /**
   * Helper function to handle a single versioned update
   * @param alreadyLocked indicates if the mutex is already held by the caller
   */
  private suspend fun tryVersionedUpdate(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long,
    currentVersion: Long,
    alreadyLocked: Boolean = false
  ): Boolean {
    // Early version check
    if (currentVersion != expectedVersion) return false

    return when {
      // Handle null bytes (deletion)
      bytes == null -> {
        if (expectedVersion == 0L) {
          storage[key] == null
        } else {
          storage.remove(key) != null
        }
      }
      // Handle non-null bytes
      else -> {
        if (expectedVersion == 0L) {
          // For version 0, use mutex to ensure atomicity if not already locked
          val operation = {
            if (storage[key] != null) {
              false
            } else {
              storage[key] = AtomicReference(VersionedValue(bytes, 1L))
              true
            }
          }
          if (alreadyLocked) operation() else mutex.withLock { operation() }
        } else {
          val ref = storage[key] ?: return false
          var success = false
          while (!success) {
            val current = ref.get()
            if (current.version != expectedVersion) {
              return false
            }
            success = ref.compareAndSet(current, VersionedValue(bytes, expectedVersion + 1L))
          }
          true
        }
      }
    }
  }

  override suspend fun putWithVersions(updates: Map<String, Pair<ByteArray?, Long>>): Map<String, Boolean> {
    // Early exit for empty updates
    if (updates.isEmpty()) return emptyMap()

    return mutex.withLock {
      // Get current state atomically without suspension points
      val currentStates = updates.keys.associateWith { key ->
        val ref = storage[key]
        if (ref != null) {
          val current = ref.get()
          Pair(current.value, current.version)
        } else {
          Pair(null, 0L)
        }
      }

      updates.entries.associate { (key, update) ->
        val (bytes, expectedVersion) = update
        val (_, currentVersion) = currentStates[key]!!
        key to tryVersionedUpdate(key, bytes, expectedVersion, currentVersion, alreadyLocked = true)
      }
    }
  }

  override suspend fun putWithVersion(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long
  ): Boolean {
    val (_, currentVersion) = getStateAndVersion(key)
    return tryVersionedUpdate(key, bytes, expectedVersion, currentVersion, alreadyLocked = false)
  }

  @TestOnly
  override fun flush() {
    storage.clear()
  }
}
