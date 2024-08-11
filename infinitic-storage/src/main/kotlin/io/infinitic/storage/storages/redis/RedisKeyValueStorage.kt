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
package io.infinitic.storage.storages.redis

import io.infinitic.storage.config.RedisConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly
import redis.clients.jedis.JedisPool

class RedisKeyValueStorage(internal val pool: JedisPool) : KeyValueStorage {

  companion object {
    fun from(config: RedisConfig) = RedisKeyValueStorage(config.getPool())
  }

  override suspend fun get(key: String): ByteArray? =
      pool.resource.use { it.get(key.toByteArray()) }

  override suspend fun put(key: String, value: ByteArray) =
      pool.resource.use {
        it.set(key.toByteArray(), value)
        Unit
      }

  override suspend fun del(key: String) =
      pool.resource.use {
        it.del(key.toByteArray())
        Unit
      }

  override fun close() {
    pool.close()
  }

  @TestOnly
  override fun flush() {
    pool.resource.use { it.flushDB() }
  }
}
