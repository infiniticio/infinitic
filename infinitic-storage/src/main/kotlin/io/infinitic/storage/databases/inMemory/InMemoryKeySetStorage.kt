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
import io.infinitic.storage.data.Bytes
import io.infinitic.storage.keySet.KeySetStorage
import org.jetbrains.annotations.TestOnly

class InMemoryKeySetStorage(internal val storage: MutableMap<String, MutableSet<Bytes>>) :
  KeySetStorage {

  companion object {
    fun from(config: InMemoryConfig) = InMemoryKeySetStorage(config.getPool().keySet)
  }

  override suspend fun get(key: String): Set<ByteArray> {
    return getBytesPerKey(key).map { it.content }.toSet()
  }

  override suspend fun add(key: String, value: ByteArray) {
    getBytesPerKey(key).add(Bytes(value))
  }

  override suspend fun remove(key: String, value: ByteArray) {
    getBytesPerKey(key).remove(Bytes(value))

    // clean key if now empty
    if (getBytesPerKey(key).isEmpty()) storage.remove(key)
  }

  override fun close() {
    // Do nothing
  }

  @TestOnly
  override fun flush() {
    storage.clear()
  }

  private fun getBytesPerKey(key: String): MutableSet<Bytes> {
    return storage[key]
      ?: run {
        val set = mutableSetOf<Bytes>()
        storage[key] = set
        set
      }
  }
}
