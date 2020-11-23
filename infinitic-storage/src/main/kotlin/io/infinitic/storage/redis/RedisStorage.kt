// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.storage.redis

import io.infinitic.common.storage.keyValue.KeyValueStorage
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class RedisStorage internal constructor(private val config: RedisStorageConfig) : KeyValueStorage {
    private val pool = JedisPool(JedisPoolConfig(), config.host, config.port)

    override fun getState(key: String): ByteBuffer? =
        pool.resource.use { jedis ->
            jedis.get(key.toByteArray(StandardCharsets.UTF_8))?.toByteBuffer()
        }

    override fun putState(key: String, value: ByteBuffer) =
        pool.resource.use { jedis ->
            jedis.set(key.toByteArray(StandardCharsets.UTF_8), value.toByteArray())

            Unit
        }

    override fun updateState(key: String, value: ByteBuffer) = putState(key, value)

    override fun deleteState(key: String) =
        pool.resource.use { jedis ->
            jedis.del(key.toByteArray(StandardCharsets.UTF_8))

            Unit
        }

    override fun incrementCounter(key: String, amount: Long) =
        pool.resource.use { jedis ->
            jedis.incrBy(key.toByteArray(StandardCharsets.UTF_8), amount)

            Unit
        }

    override fun getCounter(key: String): Long = getState(key)?.let { String(it.toByteArray()) }?.toLong() ?: 0L
}

fun ByteArray.toByteBuffer(): ByteBuffer = ByteBuffer.wrap(this)

fun ByteBuffer.toByteArray(): ByteArray {
    val byteArray = ByteArray(capacity())
    get(byteArray)

    return byteArray
}

fun redis(init: RedisStorageConfig.() -> Unit = RedisStorageConfig::defaultConfigLambda): RedisStorage {
    val config = RedisStorageConfig()
    config.init()

    return RedisStorage(config)
}

fun RedisStorageConfig.defaultConfigLambda() {}
