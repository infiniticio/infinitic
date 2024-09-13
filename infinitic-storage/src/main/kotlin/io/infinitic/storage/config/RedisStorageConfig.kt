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
import io.infinitic.storage.config.RedisConfig.PoolConfig
import io.infinitic.storage.databases.redis.RedisKeySetStorage
import io.infinitic.storage.databases.redis.RedisKeyValueStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keyValue.KeyValueStorage
import redis.clients.jedis.Protocol

data class RedisStorageConfig(
  internal val redis: RedisConfig,
  override var compression: CompressionConfig? = null,
  override var cache: CacheConfig? = null
) : StorageConfig(), RedisConfigInterface by redis {

  override val type = "redis"

  override val dbKeyValue: KeyValueStorage by lazy {
    RedisKeyValueStorage.from(redis)
  }

  override val dbKeySet: KeySetStorage by lazy {
    RedisKeySetStorage.from(redis)
  }

  companion object {
    @JvmStatic
    fun builder() = RedisStorageConfigBuilder()
  }

  /**
   * RedisStorageConfig builder
   */
  class RedisStorageConfigBuilder : StorageConfigBuilder {
    private var compression: CompressionConfig? = null
    private var cache: CacheConfig? = null
    private var host: String? = null
    private var port: Int? = null
    private var username: String? = null
    private var password: String? = null
    private var database: Int = Protocol.DEFAULT_DATABASE
    private var timeout: Int = Protocol.DEFAULT_TIMEOUT
    private var ssl: Boolean? = null
    private var poolConfig: PoolConfig? = null

    fun setCompression(compression: CompressionConfig) = apply { this.compression = compression }
    fun setCache(cache: CacheConfig) = apply { this.cache = cache }
    fun setHost(host: String) = apply { this.host = host }
    fun setPort(port: Int) = apply { this.port = port }
    fun setUsername(user: String) = apply { this.username = user }
    fun setPassword(password: String) = apply { this.password = password }
    fun setDatabase(database: Int) = apply { this.database = database }
    fun setTimeout(timeout: Int) = apply { this.timeout = timeout }
    fun setSsl(ssl: Boolean) = apply { this.ssl = ssl }
    fun setPoolConfig(poolConfig: PoolConfig) = apply { this.poolConfig = poolConfig }
    fun setPoolConfig(poolConfigBuilder: PoolConfig.PoolConfigBuilder) =
        apply { this.poolConfig = poolConfigBuilder.build() }

    override fun build(): RedisStorageConfig {
      require(host != null) { "${RedisConfig::host.name}  must not be null" }
      require(port != null) { "${RedisConfig::port.name}  must not be null" }

      val redisConfig = RedisConfig(
          host!!,
          port!!,
          username,
          password,
          database,
          timeout,
          ssl ?: false,
          poolConfig ?: PoolConfig(),
      )

      return RedisStorageConfig(
          compression = compression,
          cache = cache,
          redis = redisConfig,
      )
    }
  }
}
