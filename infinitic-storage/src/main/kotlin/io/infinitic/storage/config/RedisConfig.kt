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

import com.sksamuel.hoplite.Secret
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Protocol
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
data class RedisConfig(
  val host: String = Protocol.DEFAULT_HOST,
  var port: Int = Protocol.DEFAULT_PORT,
  var timeout: Int = Protocol.DEFAULT_TIMEOUT,
  var user: String? = null,
  var password: Secret? = null,
  var database: Int = Protocol.DEFAULT_DATABASE,
  var ssl: Boolean = false,
  var poolConfig: PoolConfig = PoolConfig()
) {

  companion object {
    @JvmStatic
    fun builder() = RedisConfigBuilder()

    private val pools = ConcurrentHashMap<RedisConfig, JedisPool>()
  }

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
    when (password?.value.isNullOrEmpty()) {
      true -> JedisPool(jedisPoolConfig, host, port, database)
      false -> JedisPool(
          jedisPoolConfig,
          host,
          port,
          timeout,
          user,
          password?.value,
          database,
          ssl,
      )
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

  /**
   * RedisConfig builder (Useful for Java user)
   */
  class RedisConfigBuilder {
    private val default = RedisConfig()
    private var host = default.host
    private var port = default.port
    private var timeout = default.timeout
    private var user = default.user
    private var password = default.password
    private var database = default.database
    private var ssl = default.ssl
    private var poolConfig = default.poolConfig

    fun setHost(host: String) = apply { this.host = host }
    fun setPort(port: Int) = apply { this.port = port }
    fun setTimeout(timeout: Int) = apply { this.timeout = timeout }
    fun setUser(user: String?) = apply { this.user = user }
    fun setPassword(password: Secret?) = apply { this.password = password }
    fun setDatabase(database: Int) = apply { this.database = database }
    fun setSsl(ssl: Boolean) = apply { this.ssl = ssl }
    fun setPoolConfig(poolConfig: PoolConfig) = apply { this.poolConfig = poolConfig }
    fun setPoolConfig(poolConfigBuilder: PoolConfig.PoolConfigBuilder) =
        apply { this.poolConfig = poolConfigBuilder.build() }

    fun build() = RedisConfig(host, port, timeout, user, password, database, ssl, poolConfig)
  }
}


