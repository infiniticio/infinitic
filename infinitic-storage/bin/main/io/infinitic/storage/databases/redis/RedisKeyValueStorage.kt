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
        jedis.watch(keyBytes, versionKeyBytes)
        // Crucially, check if the target key OR the version key already exists.
        // If either exists, we cannot perform a version 0 insert.
        if (jedis.exists(keyBytes) || jedis.exists(versionKeyBytes)) {
            jedis.unwatch()
            false
        } else {
            val transaction = jedis.multi()
            try {
                // Attempt to set the main value
                transaction.set(keyBytes, bytes)
                // Attempt to set the version ONLY if it doesn't exist (atomic check)
                transaction.setnx(versionKeyBytes, "1".toByteArray())
                val execResults = transaction.exec()

                // Transaction failed (WATCH triggered)?
                if (execResults == null) {
                    false
                } else {
                    // Check the result of SETNX (should be the second result)
                    // Should succeed only if SETNX returned 1 (meaning version key was set)
                    // Note: result type might be Long for SETNX
                    (execResults.getOrNull(1) as? Long) == 1L
                }
            } catch (e: Exception) {
                 try { transaction.discard() } catch (discardEx: Exception) { /* ignore */ }
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

  override suspend fun getStatesAndVersions(keys: Set<String>): Map<String, Pair<ByteArray?, Long>> {
    if (keys.isEmpty()) return emptyMap()

    return pool.resource.use { jedis ->
      val keyBytes = keys.map { it.toByteArray() }.toTypedArray()
      val versionKeyBytes = keys.map { "$it$VERSION_SUFFIX".toByteArray() }.toTypedArray()

      // Use a transaction to get values and versions atomically
      val transaction = jedis.multi()
      transaction.mget(*keyBytes)
      transaction.mget(*versionKeyBytes)
      val results = transaction.exec()

      // results[0] contains values, results[1] contains versions
      val values = results[0] as List<ByteArray?>
      val versions = results[1] as List<ByteArray?>

      keys.mapIndexed { index, key ->
        val value = values[index]
        val version = versions[index]?.toString(Charsets.UTF_8)?.toLongOrNull() ?: 0L
        key to Pair(value, version)
      }.toMap()
    }
  }

  override suspend fun putWithVersions(updates: Map<String, Pair<ByteArray?, Long>>): Map<String, Boolean> {
    if (updates.isEmpty()) return emptyMap()

    val maxRetries = 5

    repeat(maxRetries) { attempt ->
      val results = mutableMapOf<String, Boolean>()
      var watchFailed = false

      pool.resource.use { jedis ->
        try {
          // 1. WATCH all involved keys (data and version keys)
          val keysToWatch = updates.keys.flatMap { key ->
            listOf(key.toByteArray(), "$key$VERSION_SUFFIX".toByteArray())
          }.toTypedArray()
          jedis.watch(*keysToWatch)

          // 2. Fetch current versions within the WATCH
          val versionKeys = updates.keys.map { "$it$VERSION_SUFFIX".toByteArray() }.toTypedArray()
          val currentVersionStrings = if (versionKeys.isNotEmpty()) jedis.mget(*versionKeys) else emptyList()
          val currentVersions = updates.keys.mapIndexed { index, key ->
            key to (currentVersionStrings.getOrNull(index)?.toString(Charsets.UTF_8)?.toLongOrNull() ?: 0L)
          }.toMap()

          // 3. Check versions
          var allVersionsMatch = true
          for ((key, update) in updates) {
            val (_, expectedVersion) = update
            val currentVersion = currentVersions[key] ?: 0L
            if (currentVersion != expectedVersion) {
              allVersionsMatch = false
              results[key] = false // Mark immediate failure
            } else {
              results[key] = true // Mark potential success
            }
          }

          // If any version mismatch, unwatch and return failures
          if (!allVersionsMatch) {
            jedis.unwatch()
            return updates.keys.associateWith { results[it] ?: false } // Return immediately
          }

          // 4. Prepare and execute transaction if all versions match
          val transaction = jedis.multi()
          var transactionQueued = false
          try {
            updates.forEach { (key, update) ->
              val (bytes, expectedVersion) = update
              val keyBytes = key.toByteArray()
              val versionKeyBytes = "$key$VERSION_SUFFIX".toByteArray()

              when {
                bytes == null -> { // Deletion
                  if (expectedVersion != 0L) { // Only delete if it exists (version > 0)
                    transaction.del(keyBytes)
                    transaction.del(versionKeyBytes)
                    transactionQueued = true
                  }
                  // If expectedVersion is 0 and bytes is null, it's a success no-op.
                }
                expectedVersion == 0L -> { // Insertion
                  transaction.set(keyBytes, bytes)
                  transaction.set(versionKeyBytes, "1".toByteArray())
                  transactionQueued = true
                }
                else -> { // Update
                  transaction.set(keyBytes, bytes)
                  transaction.set(versionKeyBytes, (expectedVersion + 1).toString().toByteArray())
                  transactionQueued = true
                }
              }
            }
            
            // Only execute if operations were actually queued
            if (transactionQueued) {
              val execResults = transaction.exec()
              if (execResults == null) {
                // WATCH failed, need to retry
                watchFailed = true
              } else {
                // Transaction succeeded
                return results // Return the success map
              }
            } else {
                // No operations queued (e.g., all were deletes of non-existent keys with version 0)
                // Discard the transaction to reset Jedis state and unwatch keys
                transaction.discard()
                return results // Return the success map
            }
          } catch (e: JedisException) {
            transaction.discard()
            throw e // Re-throw Jedis specific exceptions for retry logic
          } 

        } catch (e: JedisException) {
          // Handle potential connection errors during WATCH/MGET
           if (isRetryableError(e) && attempt < maxRetries - 1) {
               watchFailed = true // Treat as a reason to retry
           } else {
               throw e
           }
        }
      } // end of use block

      // If watch failed, delay and retry
      if (watchFailed && attempt < maxRetries - 1) {
        delay(10L * (1L shl attempt))
      } else if (watchFailed) {
          // Retries exhausted after watch failure
          throw JedisException("Redis transaction failed after multiple retries due to WATCH failure.")
      }
    } // end of repeat block

    // If we somehow exit the loop without returning (e.g., initial JedisException not caught above)
    // Or if retries were exhausted for a non-watch failure initially captured
    return updates.keys.associateWith { false }
  }
}
