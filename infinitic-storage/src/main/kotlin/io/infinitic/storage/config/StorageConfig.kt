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
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.storage.compression.CompressionConfig
import io.infinitic.storage.keySet.CachedKeySetStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keyValue.CachedKeyValueStorage
import io.infinitic.storage.keyValue.CompressedKeyValueStorage
import io.infinitic.storage.keyValue.KeyValueStorage

@Suppress("MemberVisibilityCanBePrivate", "unused")
sealed class StorageConfig {
  abstract var compression: CompressionConfig?
  abstract var cache: CacheConfig?

  abstract val dbKeyValue: KeyValueStorage
  abstract val dbKeySet: KeySetStorage

  fun compression(compression: CompressionConfig) =
      apply { this.compression = compression }

  fun cache(cache: CacheConfig) =
      apply { this.cache = cache }

  val keyValue: KeyValueStorage by lazy {
    CompressedKeyValueStorage(compression, dbKeyValue).withCache()
  }

  val keySet: KeySetStorage by lazy {
    dbKeySet.withCache()
  }

  private fun KeyValueStorage.withCache() = when {
    cache?.keyValue == null -> this
    else -> CachedKeyValueStorage(cache!!.keyValue!!, this)
  }

  private fun KeySetStorage.withCache() = when {
    cache?.keySet == null -> this
    else -> CachedKeySetStorage(cache!!.keySet!!, this)
  }

  companion object {
    /** Create StorageConfig from files in file system */
    @JvmStatic
    fun fromYamlFile(vararg files: String): StorageConfig =
        loadFromYamlFile(*files)

    /** Create StorageConfig from files in resources directory */
    @JvmStatic
    fun fromYamlResource(vararg resources: String): StorageConfig =
        loadFromYamlResource(*resources)

    /** Create StorageConfig from yaml strings */
    @JvmStatic
    fun fromYamlString(vararg yamls: String): StorageConfig =
        loadFromYamlString(*yamls)
  }

  interface StorageConfigBuilder {
    fun build(): StorageConfig
  }
}
