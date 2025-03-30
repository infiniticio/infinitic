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

internal data class VersionedValue(
  val value: ByteArray,
  val version: Long
)

internal class InMemoryKeyValueStorage(
  val storage: ConcurrentHashMap<String, AtomicReference<VersionedValue>> = ConcurrentHashMap()
) : KeyValueStorage {

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

  override suspend fun putWithVersion(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long
  ): Boolean {
    if (bytes == null) {
      if (expectedVersion == 0L) return (storage[key] == null)
      // Only remove if version matches
      val ref = storage[key] ?: return false
      val current = ref.get()
      if (current.version != expectedVersion) return false
      return storage.remove(key) != null
    }

    // Handle non-null bytes case
    if (expectedVersion == 0L) {
      // For version 0, only succeed if key doesn't exist
      val existing = storage.putIfAbsent(key, AtomicReference(VersionedValue(bytes, 1L)))
      return existing == null
    }

    // Update only if version matches
    val ref = storage[key] ?: return false
    while (true) {
      val current = ref.get()
      if (current.version != expectedVersion) return false
      if (ref.compareAndSet(current, VersionedValue(bytes, expectedVersion + 1L))) {
        return true
      }
      // If CAS failed, loop will retry with new current value
    }
  }

  @TestOnly
  override fun flush() {
    storage.clear()
  }
}
