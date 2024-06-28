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
package io.infinitic.storage.databases.redis

import io.infinitic.storage.Redis
import io.infinitic.storage.keySet.KeySetStorage
import org.jetbrains.annotations.TestOnly
import redis.clients.jedis.JedisPool

class RedisKeySetStorage(internal val pool: JedisPool) : KeySetStorage {

  companion object {
    fun from(config: Redis) = RedisKeySetStorage(config.getPool())
  }

  override suspend fun get(key: String): Set<ByteArray> =
      pool.resource.use { it.smembers(key.toByteArray()) }

  override suspend fun add(key: String, value: ByteArray) {
    pool.resource.use { it.sadd(key.toByteArray(), value) }
  }

  override suspend fun remove(key: String, value: ByteArray) {
    pool.resource.use { it.srem(key.toByteArray(), value) }
  }

  override fun close() {
    pool.close()
  }

  @TestOnly
  override fun flush() {
    pool.resource.use { it.flushDB() }
  }
}
