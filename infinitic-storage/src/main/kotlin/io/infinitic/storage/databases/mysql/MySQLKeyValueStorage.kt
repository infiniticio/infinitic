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
package io.infinitic.storage.databases.mysql

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.storage.config.MySQLConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import kotlinx.coroutines.delay
import org.jetbrains.annotations.TestOnly
import java.sql.Connection

class MySQLKeyValueStorage(
  internal val pool: HikariDataSource,
  private val tableName: String
) : KeyValueStorage {

  companion object {
    fun from(config: MySQLConfig) = MySQLKeyValueStorage(config.getPool(), config.keyValueTable)
  }

  init {
    // Create table if needed
    initKeyValueTable()
  }

  override fun close() {
    pool.close()
  }

  override suspend fun get(key: String): ByteArray? =
      pool.connection.use { connection ->
        connection.prepareStatement("SELECT `value` FROM $tableName WHERE `key`=?")
            .use { statement ->
              statement.setString(1, key)
              statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                  resultSet.getBytes("value")
                } else null
              }
            }
      }

  override suspend fun put(key: String, bytes: ByteArray?) {
    pool.connection.use { connection ->
      when (bytes) {
        null -> connection.prepareStatement(
            "DELETE FROM $tableName WHERE `key`=?",
        ).use {
          it.setString(1, key)
          it.executeUpdate()
        }

        else -> connection.prepareStatement(
            "INSERT INTO $tableName (`key`, value, version) VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE value = VALUES(value), version = version + 1",
        ).use {
          it.setString(1, key)
          it.setBytes(2, bytes)
          it.executeUpdate()
        }
      }
    }
  }

  override suspend fun get(keys: Set<String>): Map<String, ByteArray?> {
    if (keys.isEmpty()) return emptyMap()

    return pool.connection.use { connection ->
      val questionMarks = keys.joinToString(",") { "?" }
      connection.prepareStatement("SELECT `key`, `value` FROM $tableName WHERE `key` IN ($questionMarks)")
          .use { statement ->
            keys.forEachIndexed { index, key -> statement.setString(index + 1, key) }
            statement.executeQuery().use { resultSet ->
              val result = mutableMapOf<String, ByteArray?>()
              while (resultSet.next()) {
                result[resultSet.getString("key")] = resultSet.getBytes("value")
              }
              // add missing keys
              keys.forEach { key ->
                result.putIfAbsent(key, null)
              }
              result
            }
          }
    }
  }

  override suspend fun put(bytes: Map<String, ByteArray?>) {
    if (bytes.isEmpty()) return

    pool.connection.use { connection ->
      // Sorting keys to ensure consistent order of access
      val sortedBytes = bytes.toSortedMap()
      connection.autoCommit = false
      connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
      try {
        // Batch DELETE using IN clause for better performance
        val keysToDelete = sortedBytes.filter { it.value == null }.keys
        if (keysToDelete.isNotEmpty()) {
          val questionMarks = keysToDelete.joinToString(",") { "?" }
          connection.prepareStatement("DELETE FROM $tableName WHERE `key` IN ($questionMarks)")
              .use { stmt ->
                keysToDelete.forEachIndexed { index, key ->
                  stmt.setString(index + 1, key)
                }
                stmt.executeUpdate()
              }
        }

        // Batch INSERT/UPDATE using VALUES for better performance
        val keysToUpsert = sortedBytes.filter { it.value != null }
        if (keysToUpsert.isNotEmpty()) {
          val valuesSql = keysToUpsert.keys.joinToString(",") { "(?, ?, 1)" }
          connection.prepareStatement(
              "INSERT INTO $tableName(`key`, value, version) VALUES $valuesSql " +
                  "ON DUPLICATE KEY UPDATE " +
                  "value = VALUES(value), " +
                  "version = version + 1",
          ).use { stmt ->
            var paramIndex = 1
            keysToUpsert.forEach { (key, value) ->
              stmt.setString(paramIndex++, key)
              stmt.setBytes(paramIndex++, value)
            }
            stmt.executeUpdate()
          }
        }
        connection.commit()
      } catch (e: Exception) {
        connection.rollback()
        throw e
      } finally {
        connection.autoCommit = true
      }
    }
  }

  override suspend fun putWithVersion(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long
  ): Boolean {
    // Maximum number of retry attempts
    val maxRetries = 5

    repeat(maxRetries) { attempt ->
      try {
        return pool.connection.use { connection ->
          connection.autoCommit = false
          connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
          try {
            // Get current version with row lock
            val currentVersion = executeSelect(connection, key)

            // Early version check
            val result = if (currentVersion != expectedVersion) {
              false
            } else {
              when {
                bytes == null -> executeDelete(connection, key, expectedVersion)
                expectedVersion == 0L -> executeInsert(connection, key, bytes, currentVersion)
                else -> executeUpdate(connection, key, bytes, expectedVersion)
              }
            }
            connection.commit()
            result
          } catch (e: Exception) {
            connection.rollback()
            throw e
          } finally {
            connection.autoCommit = true
          }
        }
      } catch (e: Exception) {
        // Check if it's a MySQL deadlock exception
        if (e.message?.contains("Deadlock found") == true && attempt < maxRetries - 1) {
          // Exponential backoff delay: 50ms, 100ms, 200ms...
          delay(50L * (1L shl attempt))
        } else {
          throw e
        }
      }
    }

    return false
  }

  override suspend fun getStatesAndVersions(keys: Set<String>): Map<String, Pair<ByteArray?, Long>> {
    if (keys.isEmpty()) return emptyMap()

    return pool.connection.use { connection ->
      val questionMarks = keys.joinToString(",") { "?" }
      connection.prepareStatement("SELECT `key`, value, version FROM $tableName WHERE `key` IN ($questionMarks)")
          .use { statement ->
            keys.forEachIndexed { index, key -> statement.setString(index + 1, key) }
            statement.executeQuery().use { resultSet ->
              val result = mutableMapOf<String, Pair<ByteArray?, Long>>()
              while (resultSet.next()) {
                result[resultSet.getString("key")] = Pair(
                    resultSet.getBytes("value"),
                    resultSet.getLong("version"),
                )
              }
              // add missing keys with version 0
              keys.forEach { key ->
                result.putIfAbsent(key, Pair(null, 0L))
              }
              result
            }
          }
    }
  }

  override suspend fun putWithVersions(updates: Map<String, Pair<ByteArray?, Long>>): Map<String, Boolean> {
    if (updates.isEmpty()) return emptyMap()

    // Maximum number of retry attempts
    val maxRetries = 5
    // Threshold for batch processing
    val batchThreshold = 10

    repeat(maxRetries) { attempt ->
      try {
        return pool.connection.use { connection ->
          connection.autoCommit = false
          connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED

          try {
            // Sort updates by key to prevent deadlocks
            val sortedUpdates = updates.entries.sortedBy { it.key }

            val results = if (updates.size <= batchThreshold) {
              // For small batches, process one by one
              processUpdatesOneByOne(connection, sortedUpdates)
            } else {
              // For larger batches, use batch processing
              processUpdatesInBatch(connection, sortedUpdates)
            }

            connection.commit()
            results
          } catch (e: Exception) {
            connection.rollback()
            throw e
          } finally {
            connection.autoCommit = true
          }
        }
      } catch (e: Exception) {
        // Check if it's a MySQL deadlock exception
        if (e.message?.contains("Deadlock found") == true && attempt < maxRetries - 1) {
          // Exponential backoff delay: 10ms, 20ms, 40ms...
          delay(10L * (1L shl attempt))
        } else {
          throw e
        }
      }
    }

    // If we reach here, all retries failed
    return updates.keys.associateWith { false }
  }


  private fun executeSelect(
    connection: Connection,
    key: String
  ): Long = connection.prepareStatement(
      "SELECT version FROM $tableName WHERE `key` = ? FOR UPDATE",
  ).use { stmt ->
    stmt.setString(1, key)
    stmt.executeQuery().use { rs ->
      if (rs.next()) rs.getLong("version") else 0L
    }
  }

  private fun executeSelectBatch(
    connection: Connection,
    keys: List<String>
  ): Map<String, Long> {
    val questionMarks = keys.joinToString(",") { "?" }
    return connection.prepareStatement(
        "SELECT `key`, version FROM $tableName WHERE `key` IN ($questionMarks) FOR UPDATE",
    ).use { stmt ->
      keys.forEachIndexed { index, key ->
        stmt.setString(index + 1, key)
      }
      stmt.executeQuery().use { rs ->
        buildMap {
          while (rs.next()) {
            put(rs.getString("key"), rs.getLong("version"))
          }
        }
      }
    }
  }

  private fun executeDelete(
    connection: Connection,
    key: String,
    expectedVersion: Long
  ): Boolean = when (expectedVersion) {
    0L -> true
    else -> connection.prepareStatement(
        "DELETE FROM $tableName WHERE `key` = ? AND version = ?",
    ).use { stmt ->
      stmt.setString(1, key)
      stmt.setLong(2, expectedVersion)
      stmt.executeUpdate() > 0
    }
  }

  private fun executeInsert(
    connection: Connection,
    key: String,
    bytes: ByteArray,
    currentVersion: Long
  ): Boolean = if (currentVersion > 0L) {
    false
  } else {
    connection.prepareStatement(
        """
        INSERT IGNORE INTO $tableName (`key`, value, version)
        VALUES (?, ?, 1)
        """.trimIndent(),
    ).use { stmt ->
      stmt.setString(1, key)
      stmt.setBytes(2, bytes)
      // For INSERT IGNORE, success means exactly 1 row was affected
      stmt.executeUpdate() == 1
    }
  }

  private fun executeUpdate(
    connection: Connection,
    key: String,
    bytes: ByteArray,
    expectedVersion: Long
  ): Boolean = connection.prepareStatement(
      "UPDATE $tableName SET value = ?, version = version + 1 WHERE `key` = ? AND version = ?",
  ).use { stmt ->
    stmt.setBytes(1, bytes)
    stmt.setString(2, key)
    stmt.setLong(3, expectedVersion)
    stmt.executeUpdate() > 0
  }

  override suspend fun getStateAndVersion(key: String): Pair<ByteArray?, Long> =
      pool.connection.use { connection ->
        connection.prepareStatement("SELECT value, version FROM $tableName WHERE `key`=?")
            .use {
              it.setString(1, key)
              it.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                  Pair(resultSet.getBytes("value"), resultSet.getLong("version"))
                } else Pair(null, 0)
              }
            }
      }

  private fun processUpdatesOneByOne(
    connection: Connection,
    updates: List<Map.Entry<String, Pair<ByteArray?, Long>>>
  ): Map<String, Boolean> {
    return updates.associate { (key, update) ->
      val (bytes, expectedVersion) = update

      // Get current version with row lock
      val currentVersion = executeSelect(connection, key)

      // Early version check
      if (currentVersion != expectedVersion) {
        key to false
      } else {
        key to when {
          bytes == null -> executeDelete(connection, key, expectedVersion)
          expectedVersion == 0L -> executeInsert(connection, key, bytes, currentVersion)
          else -> executeUpdate(connection, key, bytes, expectedVersion)
        }
      }
    }
  }

  private fun processUpdatesInBatch(
    connection: Connection,
    updates: List<Map.Entry<String, Pair<ByteArray?, Long>>>
  ): Map<String, Boolean> {
    // First get all versions with locks
    val currentVersions = executeSelectBatch(connection, updates.map { it.key })

    // Process updates in batches by operation type
    return updates.associate { (key, update) ->
      val (bytes, expectedVersion) = update
      val currentVersion = currentVersions[key] ?: 0L

      if (currentVersion != expectedVersion) {
        key to false
      } else {
        key to when {
          bytes == null -> executeDelete(connection, key, expectedVersion)
          expectedVersion == 0L -> executeInsert(connection, key, bytes, currentVersion)
          else -> executeUpdate(connection, key, bytes, expectedVersion)
        }
      }
    }
  }

  @TestOnly
  override fun flush() {
    pool.connection.use { connection ->
      connection.prepareStatement("TRUNCATE $tableName").use { it.executeUpdate() }
    }
  }

  private fun initKeyValueTable() {
    pool.connection.use { connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $tableName (" +
              "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
              "`key` VARCHAR(255) NOT NULL UNIQUE," +
              "value LONGBLOB NOT NULL," +
              "last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
              "`value_size_in_KiB` BIGINT(20) GENERATED ALWAYS AS ((length(`value`) / 1024)) STORED," +
              "version BIGINT NOT NULL DEFAULT 1" +
              ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
      ).use { it.executeUpdate() }

      // Check if index exists first
      val indexExists = connection.prepareStatement(
          "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
      ).use { stmt ->
        stmt.setString(1, tableName)
        stmt.setString(2, "value_size_index")
        stmt.executeQuery().use { rs ->
          rs.next() && rs.getInt(1) > 0
        }
      }

      if (!indexExists) {
        connection.prepareStatement(
            "CREATE INDEX value_size_index ON $tableName(value_size_in_KiB);",
        ).use { it.executeUpdate() }
      }
    }
  }
}
