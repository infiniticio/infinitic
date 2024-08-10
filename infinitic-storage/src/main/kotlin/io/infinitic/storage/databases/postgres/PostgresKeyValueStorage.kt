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
import io.infinitic.storage.PostgresConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly
import kotlin.math.ceil

class PostgresKeyValueStorage(
  internal val pool: HikariDataSource,
  private val tableName: String
) : KeyValueStorage {

  companion object {
    fun from(config: PostgresConfig) =
        PostgresKeyValueStorage(config.getPool(), config.keyValueTable)
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
        connection.prepareStatement("SELECT value FROM $tableName WHERE key=?")
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
              "INSERT INTO $tableName (key, value, value_size_in_KiB) VALUES (?, ?, ?) " +
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
      connection.prepareStatement("DELETE FROM $tableName WHERE key=?").use {
        it.setString(1, key)
        it.executeUpdate()
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
              "id BIGSERIAL PRIMARY KEY," +
              "key VARCHAR(255) NOT NULL UNIQUE," +
              "value BYTEA NOT NULL," +
              "last_update TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP," +
              "value_size_in_KiB INTEGER" +
              ");",
      ).use { it.executeUpdate() }

      connection.prepareStatement(
          "CREATE INDEX IF NOT EXISTS value_size_index ON $tableName(value_size_in_KiB);",
      ).use { it.executeUpdate() }
    }
  }
}
