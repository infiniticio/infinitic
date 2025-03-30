/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of the rights granted to you
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
import io.infinitic.storage.keyValue.KeyValueStorage
import kotlinx.coroutines.delay
import org.jetbrains.annotations.TestOnly
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisException

class RedisKeyValueStorage(internal val pool: JedisPool) : KeyValueStorage {

  companion object {
    fun from(config: RedisConfig) = RedisKeyValueStorage(config.getPool())
    private const val VERSION_SUFFIX = ":version"
  }

  override suspend fun get(key: String): ByteArray? =
      pool.resource.use { it.get(key.toByteArray()) }

  override suspend fun put(key: String, bytes: ByteArray?) {
    pool.resource.use { jedis ->
      when (bytes) {
        null -> {
          jedis.del(key.toByteArray())
          jedis.del("$key$VERSION_SUFFIX")
        }

        else -> {
          jedis.set(key.toByteArray(), bytes)
          // Initialize version to 1 if it doesn't exist
          jedis.setnx("$key$VERSION_SUFFIX", "1")
        }
      }
    }
  }

  override suspend fun putWithVersion(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long
  ): Boolean {
    val maxRetries = 5

    repeat(maxRetries) { attempt ->
      try {
        return tryPutWithVersion(key, bytes, expectedVersion)
      } catch (e: Exception) {
        // Retry on connection issues or WATCH failures
        if (e is JedisException && isRetryableError(e) && attempt < maxRetries - 1) {
          delay(10L * (1L shl attempt))
        } else {
          throw e
        }
      }
    }

    return false
  }

  private fun isRetryableError(e: JedisException): Boolean {
    // Redis specific retryable errors:
    // - WATCH failure (concurrent modification)
    // - Connection issues
    // - Cluster redirections
    return when {
      e.message?.contains("WATCH") == true -> true
      e.message?.contains("Connection") == true -> true
      e.message?.contains("MOVED") == true -> true
      e.message?.contains("ASK") == true -> true
      else -> false
    }
  }

  private fun tryPutWithVersion(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long
  ): Boolean = pool.resource.use { jedis ->
    val keyBytes = key.toByteArray()
    val versionKeyBytes = "$key$VERSION_SUFFIX".toByteArray()

    when {
      // Special case for version 0: only succeed if key doesn't exist
      expectedVersion == 0L && bytes != null -> {
        // Watch both keys first to ensure atomicity
        jedis.watch(keyBytes, versionKeyBytes)

        // Check if either key exists
        if (jedis.exists(keyBytes) || jedis.exists(versionKeyBytes)) {
          jedis.unwatch()
          false
        } else {
          val transaction = jedis.multi()
          try {
            // Set both values atomically
            transaction.set(keyBytes, bytes)
            transaction.set(versionKeyBytes, "1".toByteArray())
            transaction.exec() != null
          } catch (e: Exception) {
            transaction.discard()
            false
          }
        }
      }

      // Handle deletion
      bytes == null -> {
        // Watch both keys to ensure atomicity
        jedis.watch(keyBytes, versionKeyBytes)

        // Get version in a single operation
        val currentVersion = jedis.get(versionKeyBytes)?.let { String(it).toLong() } ?: 0L
        if (currentVersion != expectedVersion) {
          jedis.unwatch()
          false
        } else {
          val transaction = jedis.multi()
          try {
            // Delete both keys atomically
            transaction.del(keyBytes)
            transaction.del(versionKeyBytes)
            transaction.exec() != null
          } catch (e: Exception) {
            transaction.discard()
            false
          }
        }
      }

      // Normal update case
      else -> {
        // Watch both keys to ensure atomicity
        jedis.watch(keyBytes, versionKeyBytes)

        // Get both values in a single operation using mget
        val results = jedis.mget(keyBytes, versionKeyBytes)
        val value = results[0]
        val version = results[1]?.let { String(it).toLong() } ?: 0L

        if (version != expectedVersion || value == null) {
          jedis.unwatch()
          false
        } else {
          val transaction = jedis.multi()
          try {
            transaction.set(keyBytes, bytes)
            transaction.set(versionKeyBytes, (expectedVersion + 1).toString().toByteArray())
            transaction.exec() != null
          } catch (e: Exception) {
            transaction.discard()
            false
          }
        }
      }
    }
  }

  override suspend fun getStateAndVersion(key: String): Pair<ByteArray?, Long> =
      pool.resource.use { jedis ->
        val versionKey = "$key$VERSION_SUFFIX"
        val value = jedis.get(key.toByteArray())
        val version = jedis.get(versionKey)?.toLong() ?: 0
        Pair(value, version)
      }

  override suspend fun get(keys: Set<String>): Map<String, ByteArray?> =
      pool.resource.use { jedis ->
        val byteArrayKeys = keys.map { it.toByteArray() }.toTypedArray()
        val values = jedis.mget(*byteArrayKeys)
        keys.zip(values).toMap()
      }

  override suspend fun put(bytes: Map<String, ByteArray?>) {
    pool.resource.use { jedis ->
      jedis.multi().use { transaction ->
        bytes.forEach { (key, value) ->
          if (value == null) {
            transaction.del(key.toByteArray())
            transaction.del("$key$VERSION_SUFFIX")
          } else {
            transaction.set(key.toByteArray(), value)
            // Initialize version to 1 if it doesn't exist
            transaction.setnx("$key$VERSION_SUFFIX", "1")
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
}
