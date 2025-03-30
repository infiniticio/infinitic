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
package io.infinitic.storage.databases.postgres

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.storage.config.PostgresConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import kotlinx.coroutines.delay
import org.jetbrains.annotations.TestOnly
import org.postgresql.util.PSQLException
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.math.ceil

class PostgresKeyValueStorage(
  internal val pool: HikariDataSource,
  private val tableName: String,
  private val schema: String
) : KeyValueStorage {

  companion object {
    fun from(config: PostgresConfig) = PostgresKeyValueStorage(
        config.getPool(),
        config.keyValueTable,
        config.schema,
    )
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
        connection.prepareStatement("SELECT value FROM $schema.$tableName WHERE key=?")
            .use { stmt: PreparedStatement ->
              stmt.setString(1, key)
              stmt.executeQuery().use { resultSet: ResultSet ->
                if (resultSet.next()) {
                  resultSet.getBytes("value")
                } else null
              }
            }
      }

  override suspend fun put(key: String, bytes: ByteArray?) {
    pool.connection.use { connection: Connection ->
      when (bytes) {
        null -> connection.prepareStatement(
            "DELETE FROM $schema.$tableName WHERE key=?",
        ).use { stmt: PreparedStatement ->
          stmt.setString(1, key)
          stmt.executeUpdate()
        }

        else -> connection.prepareStatement(
            "INSERT INTO $schema.$tableName (key, value, value_size_in_KiB, version) VALUES (?, ?, ?, 1) " +
                "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, version = $schema.$tableName.version + 1",
        ).use { stmt: PreparedStatement ->
          stmt.setString(1, key)
          stmt.setBytes(2, bytes)
          stmt.setInt(3, ceil(bytes.size / 1024.0).toInt())
          stmt.executeUpdate()
        }
      }
    }
  }

  override suspend fun get(keys: Set<String>): Map<String, ByteArray?> {
    if (keys.isEmpty()) return emptyMap() // Handle empty case

    return pool.connection.use { connection: Connection ->
      connection.prepareStatement(
          "SELECT key, value FROM $schema.$tableName WHERE key = ANY(?)",
      ).use { statement: PreparedStatement ->
        val array = connection.createArrayOf("VARCHAR", keys.toTypedArray())
        statement.setArray(1, array)
        statement.executeQuery().use { resultSet: ResultSet ->
          val results = mutableMapOf<String, ByteArray?>()
          while (resultSet.next()) {
            results[resultSet.getString("key")] = resultSet.getBytes("value")
          }
          // add missing keys
          keys.forEach { key ->
            results.putIfAbsent(key, null)
          }
          results
        }
      }
    }
  }

  override suspend fun put(bytes: Map<String, ByteArray?>) {
    if (bytes.isEmpty()) return // Handle empty map case

    pool.connection.use { connection: Connection ->
      // Sorting keys to ensure consistent order of access
      val sortedBytes = bytes.toSortedMap()
      connection.autoCommit = false
      // Change isolation level if necessary
      connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
      try {
        // Batch DELETE
        connection
            .prepareStatement("DELETE FROM $schema.$tableName WHERE key = ?")
            .use { deleteStatement: PreparedStatement ->
              sortedBytes.filter { it.value == null }.forEach { (key, _) ->
                deleteStatement.setString(1, key)
                deleteStatement.addBatch()
              }
              deleteStatement.executeBatch()
            }
        // Batch INSERT/UPDATE with version increment
        connection
            .prepareStatement(
                "INSERT INTO $schema.$tableName(key, value, value_size_in_KiB, version) VALUES (?, ?, ?, 1) " +
                    "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, value_size_in_KiB = EXCLUDED.value_size_in_KiB, version = $schema.$tableName.version + 1",
            )
            .use { insertStatement: PreparedStatement ->
              sortedBytes.filter { it.value != null }.forEach { (key, value) ->
                insertStatement.setString(1, key)
                insertStatement.setBytes(2, value)
                insertStatement.setInt(3, ceil(value!!.size / 1024.0).toInt())
                insertStatement.addBatch()
              }
              insertStatement.executeBatch()
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
    val maxRetries = 5

    repeat(maxRetries) { attempt ->
      try {
        return tryPutWithVersion(key, bytes, expectedVersion)
      } catch (e: Exception) {
        // PostgreSQL deadlock error code is "40P01"
        if (e is PSQLException && e.sqlState == "40P01" && attempt < maxRetries - 1) {
          delay(10L * (1L shl attempt))
        } else {
          throw e
        }
      }
    }

    return false
  }

  private fun tryPutWithVersion(
    key: String,
    bytes: ByteArray?,
    expectedVersion: Long
  ): Boolean = pool.connection.use { connection ->
    connection.autoCommit = false
    try {
      val result = when (bytes) {
        null -> when (expectedVersion) { // Deletion case
          // Succeeds only if the key does not exist (version is implicitly 0)
          0L -> !connection.prepareStatement(
              "SELECT 1 FROM $schema.$tableName WHERE key = ?",
          ).use { stmt ->
            stmt.setString(1, key)
            stmt.executeQuery().next() // Returns true if key exists, false otherwise
          }

          // Delete only if version matches
          else -> connection.prepareStatement(
              "DELETE FROM $schema.$tableName WHERE key = ? AND version = ?",
          ).use { stmt: PreparedStatement ->
            stmt.setString(1, key)
            stmt.setLong(2, expectedVersion)
            stmt.executeUpdate() > 0
          }
        }

        else -> when (expectedVersion) {
          // For version 0, use INSERT ... ON CONFLICT DO NOTHING
          // This ensures atomicity and avoids race conditions
          0L -> connection.prepareStatement(
              """
                INSERT INTO $schema.$tableName (key, value, value_size_in_KiB, version)
                VALUES (?, ?, ?, 1)
                ON CONFLICT (key) DO NOTHING
                RETURNING 1
                """.trimIndent(),
          ).use { stmt: PreparedStatement ->
            stmt.setString(1, key)
            stmt.setBytes(2, bytes)
            stmt.setInt(3, ceil(bytes.size / 1024.0).toInt())
            stmt.executeQuery().use { rs -> rs.next() }
          }

          // Update only if version matches
          else -> connection.prepareStatement(
              """
                UPDATE $schema.$tableName
                SET value = ?,
                    value_size_in_KiB = ?,
                    version = version + 1
                WHERE key = ? AND version = ?
                """.trimIndent(),
          ).use { stmt: PreparedStatement ->
            stmt.setBytes(1, bytes)
            stmt.setInt(2, ceil(bytes.size / 1024.0).toInt())
            stmt.setString(3, key)
            stmt.setLong(4, expectedVersion)
            stmt.executeUpdate() > 0
          }
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

  override suspend fun getStateAndVersion(key: String): Pair<ByteArray?, Long> =
      pool.connection.use { connection: Connection ->
        connection.prepareStatement("SELECT value, version FROM $schema.$tableName WHERE key=?")
            .use { stmt: PreparedStatement ->
              stmt.setString(1, key)
              stmt.executeQuery().use { resultSet: ResultSet ->
                if (resultSet.next()) {
                  Pair(resultSet.getBytes("value"), resultSet.getLong("version"))
                } else Pair(null, 0)
              }
            }
      }

  override suspend fun getStatesAndVersions(keys: Set<String>): Map<String, Pair<ByteArray?, Long>> {
    if (keys.isEmpty()) return emptyMap()

    return pool.connection.use { connection: Connection ->
      connection.prepareStatement(
          "SELECT key, value, version FROM $schema.$tableName WHERE key = ANY(?)",
      ).use { statement: PreparedStatement ->
        val array = connection.createArrayOf("VARCHAR", keys.toTypedArray())
        statement.setArray(1, array)
        statement.executeQuery().use { resultSet: ResultSet ->
          val results = mutableMapOf<String, Pair<ByteArray?, Long>>()
          while (resultSet.next()) {
            results[resultSet.getString("key")] = Pair(
                resultSet.getBytes("value"),
                resultSet.getLong("version"),
            )
          }
          // add missing keys with version 0
          keys.forEach { key ->
            results.putIfAbsent(key, Pair(null, 0L))
          }
          results
        }
      }
    }
  }

  override suspend fun putWithVersions(updates: Map<String, Pair<ByteArray?, Long>>): Map<String, Boolean> {
    if (updates.isEmpty()) return emptyMap()

    val maxRetries = 5

    repeat(maxRetries) { attempt ->
      try {
        return pool.connection.use { connection: Connection ->
          connection.autoCommit = false
          // Use SERIALIZABLE to ensure consistency
          connection.transactionIsolation = Connection.TRANSACTION_SERIALIZABLE

          try {
            // 1. Get current versions of all keys
            val sortedKeys = updates.keys.sorted()
            val currentVersions = connection.prepareStatement(
                """
                SELECT key, version, true as exists
                FROM $schema.$tableName
                WHERE key = ANY(?)
            """,
            ).use { stmt: PreparedStatement ->
              val array = connection.createArrayOf("VARCHAR", sortedKeys.toTypedArray())
              stmt.setArray(1, array)
              stmt.executeQuery().use { rs: ResultSet ->
                buildMap {
                  while (rs.next()) {
                    put(rs.getString("key"), Pair(rs.getLong("version"), true))
                  }
                  // Add non-existent keys with version 0
                  sortedKeys.forEach { key ->
                    putIfAbsent(key, Pair(0L, false))
                  }
                }
              }
            }

            // 2. Check versions and prepare operations
            val results = mutableMapOf<String, Boolean>()
            val operations = sortedKeys.mapNotNull { key ->
              val (bytes, expectedVersion) = updates[key]!!
              val (currentVersion, exists) = currentVersions[key]!!

              if (currentVersion != expectedVersion) {
                results[key] = false
                null
              } else {
                results[key] = true
                when {
                  bytes == null -> {
                    if (expectedVersion != 0L) {
                      Triple("DELETE", key, expectedVersion)
                    } else null
                  }

                  expectedVersion == 0L && !exists -> {
                    Triple("INSERT", key, bytes)
                  }

                  else -> {
                    Triple("UPDATE", key, bytes)
                  }
                }
              }
            }

            // 3. Execute operations
            operations.forEach { (op, key, value) ->
              when (op) {
                "DELETE" -> {
                  connection.prepareStatement(
                      """
                            DELETE FROM $schema.$tableName
                            WHERE key = ? AND version = ?
                        """,
                  ).use { stmt ->
                    stmt.setString(1, key)
                    stmt.setLong(2, value as Long)
                    stmt.executeUpdate()
                  }
                }

                "INSERT" -> {
                  connection.prepareStatement(
                      """
                            INSERT INTO $schema.$tableName (key, value, value_size_in_KiB, version)
                            VALUES (?, ?, ?, 1)
                        """,
                  ).use { stmt ->
                    stmt.setString(1, key)
                    stmt.setBytes(2, value as ByteArray)
                    stmt.setInt(3, ceil(value.size / 1024.0).toInt())
                    stmt.executeUpdate()
                  }
                }

                "UPDATE" -> {
                  connection.prepareStatement(
                      """
                            UPDATE $schema.$tableName
                            SET value = ?,
                                value_size_in_KiB = ?,
                                version = version + 1
                            WHERE key = ? AND version = ?
                        """,
                  ).use { stmt ->
                    stmt.setBytes(1, value as ByteArray)
                    stmt.setInt(2, ceil(value.size / 1024.0).toInt())
                    stmt.setString(3, key)
                    stmt.setLong(4, currentVersions[key]!!.first)
                    stmt.executeUpdate()
                  }
                }
              }
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
        // PostgreSQL error codes:
        // 40P01 - deadlock
        // 40001 - serialization failure
        if ((e is PSQLException &&
              (e.sqlState == "40P01" || e.sqlState == "40001")) &&
          attempt < maxRetries - 1) {
          delay(10L * (1L shl attempt))
        } else {
          throw e
        }
      }
    }
    // If we reach here, all retries failed
    return updates.keys.associateWith { false }
  }

  @TestOnly
  override fun flush() {
    pool.connection.use { connection: Connection ->
      connection.prepareStatement("TRUNCATE $schema.$tableName").use { stmt: PreparedStatement ->
        stmt.executeUpdate()
      }
    }
  }

  private fun initKeyValueTable() {
    pool.connection.use { connection: Connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $schema.$tableName (" +
              "id BIGSERIAL PRIMARY KEY," +
              "key VARCHAR(255) NOT NULL UNIQUE," +
              "value BYTEA NOT NULL," +
              "last_update TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP," +
              "value_size_in_KiB INTEGER," +
              "version BIGINT NOT NULL DEFAULT 1" +
              ");",
      ).use { stmt: PreparedStatement -> stmt.executeUpdate() }

      connection.prepareStatement(
          "CREATE INDEX IF NOT EXISTS value_size_index ON $schema.$tableName(value_size_in_KiB);",
      ).use { stmt: PreparedStatement -> stmt.executeUpdate() }
    }
  }
}
