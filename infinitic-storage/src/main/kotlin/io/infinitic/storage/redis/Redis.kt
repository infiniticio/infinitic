/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.storage.redis

import com.sksamuel.hoplite.Secret
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Protocol
import java.util.concurrent.ConcurrentHashMap

data class Redis(
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
        val pools = ConcurrentHashMap<Redis, JedisPool>()

        fun close() {
            pools.values.forEach { it.close() }
            pools.clear()
        }
    }

    fun getPool(
        jedisPoolConfig: JedisPoolConfig = JedisPoolConfig().also {
            it.maxTotal = poolConfig.maxTotal
            it.maxIdle = poolConfig.maxIdle
            it.minIdle = poolConfig.minIdle
        }
    ) = pools.computeIfAbsent(this) {
        when (it.password?.value.isNullOrEmpty()) {
            true -> JedisPool(
                jedisPoolConfig,
                it.host,
                it.port,
                it.database
            )

            false -> JedisPool(
                jedisPoolConfig,
                it.host,
                it.port,
                it.timeout,
                it.user,
                it.password?.value,
                it.database,
                it.ssl
            )
        }.also { pool ->
            Runtime.getRuntime().addShutdownHook(Thread { pool.close() })
        }
    }
}

data class PoolConfig(
    var maxTotal: Int = -1,
    var maxIdle: Int = 8,
    var minIdle: Int = 0
)
