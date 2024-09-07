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
import io.infinitic.cache.config.CaffeineCacheConfig
import io.infinitic.config.loadFromYamlFile
import io.infinitic.config.loadFromYamlResource
import io.infinitic.config.loadFromYamlString
import io.infinitic.storage.compression.CompressionConfig
import io.infinitic.storage.databases.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.databases.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.databases.mysql.MySQLKeySetStorage
import io.infinitic.storage.databases.mysql.MySQLKeyValueStorage
import io.infinitic.storage.databases.postgres.PostgresKeySetStorage
import io.infinitic.storage.databases.postgres.PostgresKeyValueStorage
import io.infinitic.storage.databases.redis.RedisKeySetStorage
import io.infinitic.storage.databases.redis.RedisKeyValueStorage
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
    @JvmStatic
    fun builder() = StorageConfigBuilder()

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

  class StorageConfigBuilder {
    private var compression: CompressionConfig? = null
    private var cache: CacheConfig? = null
    private var database: DatabaseConfig? = null

    fun copy() = builder().apply {
      this.compression = this@StorageConfigBuilder.compression
      this.cache = this@StorageConfigBuilder.cache
      this.database = this@StorageConfigBuilder.database
    }

    fun setCompression(compression: CompressionConfig) = apply { this.compression = compression }

    fun setCache(caffeine: CaffeineCacheConfig) =
        apply { this.cache = caffeine }

    fun setCache(caffeine: CaffeineCacheConfig.CaffeineConfigBuilder) =
        apply { this.cache = caffeine.build() }

    fun setDatabase(mysql: MySQLConfig) =
        apply { this.database = mysql }

    fun setDatabase(mysql: MySQLConfig.MySQLConfigBuilder) =
        setDatabase(mysql.build())

    fun setDatabase(postgres: PostgresConfig) =
        apply { this.database = postgres }

    fun setDatabase(postgres: PostgresConfig.PostgresConfigBuilder) =
        setDatabase(postgres.build())

    fun setDatabase(redis: RedisConfig) =
        apply { this.database = redis }

    fun setDatabase(redis: RedisConfig.RedisConfigBuilder) =
        setDatabase(redis.build())

    fun setDatabase(inMemory: InMemoryConfig) = apply { this.database = inMemory }


    fun build(): StorageConfig {
      require(database != null) { "A database configuration must be provided before building the StorageConfig." }

      return when (database!!) {
        is InMemoryConfig -> InMemoryStorageConfig(database as InMemoryConfig, compression, cache)
        is MySQLConfig -> MySQLStorageConfig(database as MySQLConfig, compression, cache)
        is PostgresConfig -> PostgresStorageConfig(database as PostgresConfig, compression, cache)
        is RedisConfig -> RedisStorageConfig(database as RedisConfig, compression, cache)
      }
    }
  }
}

data class InMemoryStorageConfig(
  internal val inMemory: InMemoryConfig,
  override var compression: CompressionConfig? = null,
  override var cache: CacheConfig? = null
) : StorageConfig() {

  override val dbKeySet: KeySetStorage by lazy {
    InMemoryKeySetStorage.from(inMemory)
  }

  override val dbKeyValue: KeyValueStorage by lazy {
    InMemoryKeyValueStorage.from(inMemory)
  }
}

data class RedisStorageConfig(
  internal val redis: RedisConfig,
  override var compression: CompressionConfig? = null,
  override var cache: CacheConfig? = null
) : StorageConfig() {
  override val dbKeyValue: KeyValueStorage by lazy {
    RedisKeyValueStorage.from(redis)
  }

  override val dbKeySet: KeySetStorage by lazy {
    RedisKeySetStorage.from(redis)
  }
}

data class MySQLStorageConfig(
  internal val mysql: MySQLConfig,
  override var compression: CompressionConfig? = null,
  override var cache: CacheConfig? = null
) : StorageConfig() {
  override val dbKeyValue: KeyValueStorage by lazy {
    MySQLKeyValueStorage.from(mysql)
  }

  override val dbKeySet: KeySetStorage by lazy {
    MySQLKeySetStorage.from(mysql)
  }
}

data class PostgresStorageConfig(
  internal val postgres: PostgresConfig,
  override var compression: CompressionConfig? = null,
  override var cache: CacheConfig? = null
) : StorageConfig() {
  override val dbKeyValue: KeyValueStorage by lazy {
    PostgresKeyValueStorage.from(postgres)
  }

  override val dbKeySet: KeySetStorage by lazy {
    PostgresKeySetStorage.from(postgres)
  }
}
