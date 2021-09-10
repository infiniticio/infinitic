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

import io.infinitic.common.storage.keyCounter.KeyCounterStorage
import org.jetbrains.annotations.TestOnly
import redis.clients.jedis.JedisPool

class RedisKeyCounterStorage(
    private val pool: JedisPool
) : KeyCounterStorage {

    companion object {
        fun of(config: Redis) = RedisKeyCounterStorage(getPool(config))
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread { pool.close() })
    }

    override suspend fun get(key: String): Long =
        pool.resource.use { it.get(key.toByteArray()) }
            ?.let { String(it) }?.toLong() ?: 0L

    override suspend fun incr(key: String, amount: Long) =
        pool.resource.use { it.incrBy(key.toByteArray(), amount); Unit }

    fun test(key: String, amount: Long) =
        pool.resource.use { it.incrBy(key.toByteArray(), amount); Unit }

    @TestOnly
    override fun flush() {
        pool.resource.use { it.flushDB() }
    }
}
