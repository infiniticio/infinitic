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

import io.infinitic.storage.config.RedisConfig
import io.infinitic.storage.keySet.KeySetPage
import io.infinitic.storage.keySet.KeySetStorage
import org.jetbrains.annotations.TestOnly
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.ScanParams
import java.util.Base64

class RedisKeySetStorage(internal val pool: JedisPool) : KeySetStorage {

  companion object {
    private const val BUFFERED_CURSOR_PREFIX = "buffered:"

    fun from(config: RedisConfig) = RedisKeySetStorage(config.getPool())
  }

  override suspend fun get(key: String): Set<ByteArray> =
      pool.resource.use { jedis ->
        jedis.smembers(key.toByteArray())
      }

  override suspend fun getPage(
    key: String,
    limit: Int,
    cursor: String?,
  ): KeySetPage {
    require(limit > 0) { "limit must be positive" }

    val state = RedisPageCursor.from(cursor)
    val values = mutableListOf<ByteArray>()
    val pendingValues = ArrayDeque(state.pendingValues)

    return pool.resource.use { jedis ->
      var backendCursor = state.backendCursor
      pendingValues.drainInto(values, limit)

      while (values.size < limit && backendCursor != null) {
        val (pageValues, nextBackendCursor) = readPage(jedis, key, backendCursor, limit - values.size)
        pendingValues.addAll(pageValues)
        pendingValues.drainInto(values, limit)
        backendCursor = nextBackendCursor
      }

      KeySetPage(
          values = values,
          nextCursor = RedisPageCursor(backendCursor, pendingValues.toList()).serialize(),
      )
    }
  }

  override suspend fun add(key: String, value: ByteArray) {
    pool.resource.use { jedis ->
      jedis.sadd(key.toByteArray(), value)
    }
  }

  override suspend fun remove(key: String, value: ByteArray) {
    pool.resource.use { jedis ->
      jedis.srem(key.toByteArray(), value)
    }
  }

  override suspend fun get(keys: Set<String>): Map<String, Set<ByteArray>> {
    return pool.resource.use { jedis ->
      val pipeline = jedis.pipelined()
      val responses = keys.associateWith { pipeline.smembers(it.toByteArray()) }
      pipeline.sync()
      responses.mapValues { it.value.get() }
    }
  }

  override suspend fun update(
    add: Map<String, Set<ByteArray>>,
    remove: Map<String, Set<ByteArray>>
  ) {
    pool.resource.use { jedis ->
      jedis.multi().use { transaction ->

        // Batch add operations for each key
        add.forEach { (key, values) ->
          if (values.isNotEmpty()) {
            transaction.sadd(key.toByteArray(), *values.toTypedArray())
          }
        }

        // Batch remove operations for each key
        remove.forEach { (key, values) ->
          if (values.isNotEmpty()) {
            transaction.srem(key.toByteArray(), *values.toTypedArray())
          }
        }

        transaction.exec()
      }
    }
  }

  override fun close() {
    pool.close()
  }

  @TestOnly
  override fun flush() {
    pool.resource.use { it.flushDB() }
  }

  private fun readPage(
    jedis: redis.clients.jedis.Jedis,
    key: String,
    cursor: String,
    limit: Int,
  ): Pair<List<ByteArray>, String?> {
    val result = jedis.sscan(
        key.toByteArray(),
        cursor.toByteArray(),
        ScanParams().count(limit),
    )

    return result.getResult() to result.getCursor().takeUnless { it == ScanParams.SCAN_POINTER_START }
  }

  private fun ArrayDeque<ByteArray>.drainInto(
    values: MutableList<ByteArray>,
    limit: Int,
  ) {
    while (values.size < limit && isNotEmpty()) {
      values += removeFirst()
    }
  }

  private data class RedisPageCursor(
    val backendCursor: String?,
    val pendingValues: List<ByteArray>,
  ) {
    companion object {
      fun from(cursor: String?): RedisPageCursor {
        if (cursor == null) {
          return RedisPageCursor(
              backendCursor = ScanParams.SCAN_POINTER_START,
              pendingValues = emptyList(),
          )
        }

        if (!cursor.startsWith(BUFFERED_CURSOR_PREFIX)) {
          return RedisPageCursor(
              backendCursor = cursor,
              pendingValues = emptyList(),
          )
        }

        val payload = cursor.removePrefix(BUFFERED_CURSOR_PREFIX)
        val parts = payload.split(":", limit = 2)
        val backendCursor = parts.first().ifEmpty { null }
        val pendingValues = parts.getOrNull(1)
            ?.takeIf { it.isNotEmpty() }
            ?.split(",")
            ?.map { Base64.getUrlDecoder().decode(it) }
            ?: emptyList()

        return RedisPageCursor(
            backendCursor = backendCursor,
            pendingValues = pendingValues,
        )
      }
    }

    fun serialize(): String? {
      if (pendingValues.isEmpty()) return backendCursor

      val encodedValues = pendingValues.joinToString(",") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
      }

      return "$BUFFERED_CURSOR_PREFIX${backendCursor.orEmpty()}:$encodedValues"
    }
  }
}
