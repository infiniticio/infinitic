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

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.cache.keyValue.CachedKeyValue
import org.jetbrains.annotations.TestOnly

open class CachedKeyValueStorage(
  private val cache: CachedKeyValue<ByteArray>,
  private val storage: KeyValueStorage
) : KeyValueStorage {

  private val logger = KotlinLogging.logger {}

  override suspend fun get(key: String): ByteArray? {
    return cache.getValue(key) ?: run {
      logger.debug { "key $key - getValue - absent from cache, get from storage" }
      storage.get(key)?.also { cache.putValue(key, it) }
    }
  }

  override suspend fun put(key: String, bytes: ByteArray?) {
    storage.put(key, bytes)
    cache.putValue(key, bytes)
  }

  override suspend fun get(keys: Set<String>): Map<String, ByteArray?> {
    val output = keys.associateWith { cache.getValue(it) }.toMutableMap()
    val missingKeys = output.filterValues { it == null }.keys
    val missingValuesFromStorage = storage.get(missingKeys)

    missingValuesFromStorage.forEach { (key, value) ->
      logger.debug { "key $key - getValue - absent from cache, get from storage" }
      value?.let {
        output[key] = value
        cache.putValue(key, value)
      }
    }

    return output
  }

  override suspend fun put(bytes: Map<String, ByteArray?>) {
    storage.put(bytes)
    bytes.forEach { (key, value) -> cache.putValue(key, value) }
  }

  override suspend fun putWithVersion(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long
  ): Boolean {
    // Try to update storage first
    val success = storage.putWithVersion(key, bytes, expectedVersion)

    // If storage update succeeded, update cache
    if (success) {
      cache.putValue(key, bytes)
    }

    return success
  }

  override suspend fun getStateAndVersion(key: String): Pair<ByteArray?, Long> {
    // We must get state and version atomically from storage
    // Cannot use cache here as we need the version
    return storage.getStateAndVersion(key)
  }

  override fun close() {
    storage.close()
  }

  @TestOnly
  override fun flush() {
    cache.flush()
    storage.flush()
  }
}
