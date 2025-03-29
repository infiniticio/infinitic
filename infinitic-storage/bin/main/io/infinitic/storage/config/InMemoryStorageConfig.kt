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

import io.infinitic.cache.config.CacheConfig
import io.infinitic.storage.compression.CompressionConfig
import io.infinitic.storage.databases.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.databases.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keyValue.KeyValueStorage

data class InMemoryStorageConfig(
  internal val inMemory: InMemoryConfig,
  override var compression: CompressionConfig? = null,
  override var cache: CacheConfig? = null
) : StorageConfig() {
  override val dbKeySet: KeySetStorage by lazy {
    InMemoryKeySetStorage.from(inMemory)
  }
  override val type = "inMemory"

  override val dbKeyValue: KeyValueStorage by lazy {
    InMemoryKeyValueStorage.from(inMemory)
  }

  companion object {
    @JvmStatic
    fun builder() = InMemoryConfigBuilder()
  }

  /**
   * InMemoryStorageConfig builder
   */
  class InMemoryConfigBuilder : StorageConfigBuilder {
    private var compression: CompressionConfig? = null
    private var cache: CacheConfig? = null

    fun setCompression(compression: CompressionConfig) = apply { this.compression = compression }
    fun setCache(cache: CacheConfig) = apply { this.cache = cache }

    override fun build() = InMemoryStorageConfig(
        compression = compression,
        cache = cache,
        inMemory = InMemoryConfig(),
    )
  }
}
