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
import io.infinitic.storage.keySet.CachedKeySetStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keyValue.CachedKeyValueStorage
import io.infinitic.storage.keyValue.CompressedKeyValueStorage
import io.infinitic.storage.keyValue.KeyValueStorage
import io.infinitic.storage.storages.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.storages.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.storages.mysql.MySQLKeySetStorage
import io.infinitic.storage.storages.mysql.MySQLKeyValueStorage
import io.infinitic.storage.storages.postgres.PostgresKeySetStorage
import io.infinitic.storage.storages.postgres.PostgresKeyValueStorage
import io.infinitic.storage.storages.redis.RedisKeySetStorage
import io.infinitic.storage.storages.redis.RedisKeyValueStorage

@Suppress("unused")
data class StorageConfig(
  private var inMemory: InMemoryConfig? = null,
  private val redis: RedisConfig? = null,
  private val mysql: MySQLConfig? = null,
  private val postgres: PostgresConfig? = null,
  var compression: CompressionConfig? = null,
  var cache: CacheConfig? = null
) {
  init {
    val nonNul = listOfNotNull(inMemory, redis, mysql, postgres)

    if (nonNul.isEmpty()) {
      // default storage is inMemory
      inMemory = InMemoryConfig()
    } else {
      require(nonNul.count() == 1) { "Storage should have only one definition: ${nonNul.joinToString { it::class.java.simpleName }}" }
    }
  }

  companion object {
    @JvmStatic
    fun builder() = StorageConfigBuilder()
  }

  fun close() {
    when {
      inMemory != null -> inMemory!!.close()
      redis != null -> redis.close()
      mysql != null -> mysql.close()
      postgres != null -> postgres.close()
      else -> thisShouldNotHappen()
    }
  }

  val type by lazy {
    when {
      inMemory != null -> StorageType.IN_MEMORY
      redis != null -> StorageType.REDIS
      mysql != null -> StorageType.MYSQL
      postgres != null -> StorageType.POSTGRES
      else -> thisShouldNotHappen()
    }
  }

  val keySet: KeySetStorage by lazy {
    when {
      inMemory != null -> InMemoryKeySetStorage.from(inMemory!!)
      redis != null -> RedisKeySetStorage.from(redis)
      mysql != null -> MySQLKeySetStorage.from(mysql)
      postgres != null -> PostgresKeySetStorage.from(postgres)
      else -> thisShouldNotHappen()
    }.withCache()
  }

  val keyValue: KeyValueStorage by lazy {
    when {
      inMemory != null -> InMemoryKeyValueStorage.from(inMemory!!)
      redis != null -> RedisKeyValueStorage.from(redis)
      mysql != null -> MySQLKeyValueStorage.from(mysql)
      postgres != null -> PostgresKeyValueStorage.from(postgres)
      else -> thisShouldNotHappen()
    }.let { CompressedKeyValueStorage(compression, it) }.withCache()
  }

  /**
   * StorageConfig builder (Useful for Java user)
   */
  class StorageConfigBuilder {
    private var inMemory: InMemoryConfig? = null
    private var redis: RedisConfig? = null
    private var mysql: MySQLConfig? = null
    private var postgres: PostgresConfig? = null
    private var compression: CompressionConfig? = null
    private var cache: CacheConfig? = null

    fun inMemory(inMemory: InMemoryConfig) = apply { this.inMemory = inMemory }
    fun redis(redis: RedisConfig) = apply { this.redis = redis }
    fun mysql(mysql: MySQLConfig) = apply { this.mysql = mysql }
    fun postgres(postgres: PostgresConfig) = apply { this.postgres = postgres }
    fun compression(compression: CompressionConfig) = apply { this.compression = compression }
    fun cache(cache: CacheConfig) = apply { this.cache = cache }

    fun build() = StorageConfig(inMemory, redis, mysql, postgres, compression, cache)
  }

  private fun thisShouldNotHappen(): Nothing {
    throw RuntimeException("This should not happen")
  }

  private fun KeyValueStorage.withCache() = when {
    cache?.keyValue == null -> this
    else -> CachedKeyValueStorage(cache!!.keyValue!!, this)
  }

  private fun KeySetStorage.withCache() = when {
    cache?.keySet == null -> this
    else -> CachedKeySetStorage(cache!!.keySet!!, this)
  }

  enum class StorageType {
    IN_MEMORY,
    REDIS,
    POSTGRES,
    MYSQL
  }
}
