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

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Protocol
import java.util.concurrent.ConcurrentHashMap

/**
 * RedisConfig is a data class that represents the configuration for connecting to a Redis server.
 *
 * @property host The Redis server host address.
 * @property port The Redis server port number.
 * @property username The username to authenticate with Redis server (optional).
 * @property password The password to authenticate with Redis server (optional).
 * @property database The Redis database index (default is 0).
 * @property timeout The timeout in milliseconds for socket connection timeout (default is 2000 milliseconds).
 * @property ssl A flag indicating whether to use SSL for the connection (default is false).
 * @property poolConfig The configuration for the connection pool (default is PoolConfig with default values).
 *
 * Example for local development:
 *
 * ```kotlin
 * RedisConfig("localhost", 6379)
 * ```
 *
 * ```java
 * RedisConfig.builder()
 *  .setHost("localhost")
 *  .setPort(6379)
 *  .build();
 * ```
 */

interface RedisConfigInterface {
  val host: String
  val port: Int
  val username: String?
  val password: String?
  val database: Int
  val timeout: Int
  val ssl: Boolean
  val poolConfig: RedisConfig.PoolConfig
}

@Suppress("unused")
data class RedisConfig(
  override val host: String,
  override val port: Int,
  override val username: String? = null,
  override val password: String? = null,
  override val database: Int = Protocol.DEFAULT_DATABASE,
  override val timeout: Int = Protocol.DEFAULT_TIMEOUT,
  override val ssl: Boolean = false,
  override val poolConfig: PoolConfig = PoolConfig()
) : RedisConfigInterface {

  companion object {
    private val pools = ConcurrentHashMap<RedisConfig, JedisPool>()
  }

  init {
    require(host.isNotBlank()) { "Invalid value for '${::host.name}': $host. The value must not be blank." }
    require(port > 0) { "Invalid value for '${::port.name}': $port. The value must be > 0." }
    require(database >= 0) { "Invalid value for '${::database.name}': $database. The value must be >= 0." }
    require(timeout > 0) { "Invalid value for '${::timeout.name}': $timeout. The value must be > 0." }
  }

  /**
   * Returns a string representation of the `PostgresConfig` object with an obfuscated password property.
   * The optional properties are included only if they have non-null values.
   */
  override fun toString() =
      "${this::class.java.simpleName}(host='$host', port=$port" +
          (username?.let { ", username='$it'" } ?: "") +
          (password?.let { ", password='******'" } ?: "") +
          ", database=$database, timeout=$timeout, ssl=$ssl, poolConfig=$poolConfig)"

  fun close() {
    pools[this]?.close()
    pools.remove(this)
  }

  fun getPool(
    jedisPoolConfig: JedisPoolConfig = JedisPoolConfig().also {
      it.maxTotal = poolConfig.maxTotal
      it.maxIdle = poolConfig.maxIdle
      it.minIdle = poolConfig.minIdle
    }
  ): JedisPool = pools.getOrPut(this) {
    when (password.isNullOrEmpty()) {
      true -> JedisPool(jedisPoolConfig, host, port, timeout, ssl)
      false -> JedisPool(jedisPoolConfig, host, port, timeout, username, password, database, ssl)
    }
  }

  data class PoolConfig(var maxTotal: Int = -1, var maxIdle: Int = 8, var minIdle: Int = 0) {
    companion object {
      @JvmStatic
      fun builder() = PoolConfigBuilder()
    }

    /**
     * PoolConfig builder (Useful for Java user)
     */
    class PoolConfigBuilder {
      private val default = PoolConfig()
      private var maxTotal = default.maxTotal
      private var maxIdle = default.maxIdle
      private var minIdle = default.minIdle

      fun setMaxTotal(maxTotal: Int) = apply { this.maxTotal = maxTotal }
      fun setMaxIdle(maxIdle: Int) = apply { this.maxIdle = maxIdle }
      fun setMinIdle(minIdle: Int) = apply { this.minIdle = minIdle }

      fun build() = PoolConfig(maxTotal, maxIdle, minIdle)
    }
  }
}
