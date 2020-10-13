package io.infinitic.storage.redis

import io.infinitic.storage.api.Storage
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.nio.ByteBuffer

class RedisStorage internal constructor(private val config: RedisStorageConfig) : Storage {
    private val pool by lazy {
        JedisPool(JedisPoolConfig(), config.host, config.port)
    }

    override fun getState(key: String): ByteBuffer? =
        pool.resource.use { jedis ->
            jedis.get(key.toByteArray())?.toByteBuffer()
        }

    override fun putState(key: String, value: ByteBuffer) =
        pool.resource.use { jedis ->
            jedis.set(key.toByteArray(), value.toByteArray())

            Unit
        }

    override fun updateState(key: String, value: ByteBuffer) = putState(key, value)

    override fun deleteState(key: String) =
        pool.resource.use { jedis ->
            jedis.del(key)

            Unit
        }

    override fun incrementCounter(key: String, amount: Long) =
        pool.resource.use { jedis ->
            jedis.incrBy(key, amount)

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

fun redis(init: RedisStorageConfig.() -> Unit = RedisStorageConfig::defaultConfig): RedisStorage {
    val config = RedisStorageConfig()
    config.init()

    return RedisStorage(config)
}

fun RedisStorageConfig.defaultConfig() {}
