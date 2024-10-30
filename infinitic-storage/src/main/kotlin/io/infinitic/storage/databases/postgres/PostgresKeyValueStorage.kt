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
package io.infinitic.storage.databases.postgres

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.storage.config.PostgresConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly
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
            .use {
              it.setString(1, key)
              it.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                  resultSet.getBytes("value")
                } else null
              }
            }
      }

  override suspend fun put(key: String, value: ByteArray) {
    pool.connection.use { connection ->
      connection
          .prepareStatement(
              "INSERT INTO $schema.$tableName (key, value, value_size_in_KiB) VALUES (?, ?, ?) " +
                  "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value",
          )
          .use {
            it.setString(1, key)
            it.setBytes(2, value)
            it.setInt(3, ceil(value.size / 1024.0).toInt())
            it.executeUpdate()
          }
    }
  }

  override suspend fun del(key: String) {
    pool.connection.use { connection ->
      connection.prepareStatement("DELETE FROM $schema.$tableName WHERE key=?").use {
        it.setString(1, key)
        it.executeUpdate()
      }
    }
  }


  override suspend fun getSet(keys: Set<String>): Map<String, ByteArray?> {
    return pool.connection.use { connection ->
      connection.prepareStatement(
          "SELECT key, value FROM $schema.$tableName WHERE key = ANY(?)",
      ).use { statement ->
        val array = connection.createArrayOf("VARCHAR", keys.toTypedArray())
        statement.setArray(1, array)
        statement.executeQuery().use { resultSet ->
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

  override suspend fun putSet(map: Map<String, ByteArray>) {
    pool.connection.use { connection ->
      connection.prepareStatement(
          "INSERT INTO $schema.$tableName (key, value, value_size_in_KiB) VALUES (?, ?, ?) " +
              "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value",
      ).use { statement ->
        for ((key, value) in map) {
          statement.setString(1, key)
          statement.setBytes(2, value)
          statement.setInt(3, ceil(value.size / 1024.0).toInt())
          statement.addBatch()
        }
        statement.executeBatch()
      }
    }
  }

  override suspend fun delSet(keys: Set<String>) {
    pool.connection.use { connection ->
      connection.prepareStatement(
          "DELETE FROM $schema.$tableName WHERE key = ANY(?)",
      ).use { statement ->
        val array = connection.createArrayOf("VARCHAR", keys.toTypedArray())
        statement.setArray(1, array)
        statement.executeUpdate()
      }
    }
  }

  @TestOnly
  override fun flush() {
    pool.connection.use { connection ->
      connection.prepareStatement("TRUNCATE $schema.$tableName").use { it.executeUpdate() }
    }
  }

  private fun initKeyValueTable() {
    pool.connection.use { connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $schema.$tableName (" +
              "id BIGSERIAL PRIMARY KEY," +
              "key VARCHAR(255) NOT NULL UNIQUE," +
              "value BYTEA NOT NULL," +
              "last_update TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP," +
              "value_size_in_KiB INTEGER" +
              ");",
      ).use { it.executeUpdate() }

      connection.prepareStatement(
          "CREATE INDEX IF NOT EXISTS value_size_index ON $schema.$tableName(value_size_in_KiB);",
      ).use { it.executeUpdate() }
    }
  }
}
