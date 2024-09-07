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
package io.infinitic.storage.config

import io.infinitic.storage.data.Bytes
import java.util.concurrent.ConcurrentHashMap

data class InMemoryConfig(private val type: String = "unused") : DatabaseConfig {

  constructor() : this("unused")

  companion object {
    val pools = ConcurrentHashMap<InMemoryConfig, InMemoryPool>()

    fun close() {
      pools.keys.forEach { it.close() }
    }
  }

  fun getPool(): InMemoryPool = pools.getOrPut(this) { InMemoryPool() }

  fun close() {
    pools[this]?.close()
    pools.remove(this)
  }

  /**
   * InMemoryPool class represents a pool for storing key-value and key-set pairs in memory.
   */
  class InMemoryPool {
    private val _keySet = mutableMapOf<String, MutableSet<Bytes>>()
    private val _keyValue = ConcurrentHashMap<String, ByteArray>()

    internal val keySet get() = _keySet

    internal val keyValue get() = _keyValue

    fun close() {
      _keyValue.clear()
      _keySet.clear()
    }
  }
}

