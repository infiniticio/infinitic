/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
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

  override suspend fun put(key: String, value: ByteArray) {
    storage.put(key, value)
    cache.putValue(key, value)
  }

  override suspend fun del(key: String) {
    storage.del(key)
    cache.delValue(key)
  }

  override suspend fun getSet(keys: Set<String>): Map<String, ByteArray?> {
    val output = keys.associateWith { cache.getValue(it) }.toMutableMap()
    val missingKeys = output.filterValues { it == null }.keys
    val missingValuesFromStorage = storage.getSet(missingKeys)

    missingValuesFromStorage.forEach { (key, value) ->
      logger.debug { "key $key - getValue - absent from cache, get from storage" }
      value?.let {
        output[key] = value
        cache.putValue(key, value)
      }
    }

    return output
  }

  override suspend fun putSet(map: Map<String, ByteArray>) {
    storage.putSet(map)
    map.forEach { (key, value) -> cache.putValue(key, value) }
  }

  override suspend fun delSet(keys: Set<String>) {
    storage.delSet(keys)
    keys.forEach { cache.delValue(it) }
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
