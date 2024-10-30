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
package io.infinitic.storage.databases.inMemory

import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

class InMemoryKeyValueStorage(internal val storage: ConcurrentHashMap<String, ByteArray>) :
  KeyValueStorage {

  companion object {
    fun from(config: InMemoryConfig) = InMemoryKeyValueStorage(config.getPool().keyValue)
  }

  override suspend fun get(key: String): ByteArray? {
    return storage[key]
  }

  override suspend fun put(key: String, value: ByteArray) {
    storage[key] = value
  }

  override suspend fun del(key: String) {
    storage.remove(key)
  }

  override suspend fun getSet(keys: Set<String>): Map<String, ByteArray?> {
    return keys.associateWith { get(it) }
  }

  override suspend fun putSet(map: Map<String, ByteArray>) {
    map.forEach { (k, v) -> put(k, v) }
  }

  override suspend fun delSet(keys: Set<String>) {
    keys.forEach { del(it) }
  }

  override fun close() {
    // Do nothing
  }

  @TestOnly
  override fun flush() {
    storage.clear()
  }

}
