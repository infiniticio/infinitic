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
import io.infinitic.storage.config.Postgres
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly
import kotlin.math.ceil

private const val KEY_VALUE_TABLE = "key_value_storage"

class PostgresKeyValueStorage(
  private val pool: HikariDataSource,
  tablePrefix: String
) : KeyValueStorage {

  companion object {
    fun from(config: Postgres) = PostgresKeyValueStorage(config.getPool(), config.tablePrefix)
  }

  // table's name
  val table =
      (if (tablePrefix.isEmpty()) KEY_VALUE_TABLE else "${tablePrefix}_$KEY_VALUE_TABLE").also {
        if (!it.isValidPostgresTableName()) throw IllegalArgumentException("$it is not a valid Postgres table name")
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
        connection.prepareStatement("SELECT value FROM $table WHERE key=?")
            .use {
              it.setString(1, key)
              it.executeQuery().use {
                if (it.next()) {
                  it.getBytes("value")
                } else null
              }
            }
      }

  override suspend fun put(key: String, value: ByteArray) {
    pool.connection.use { connection ->
      connection
          .prepareStatement(
              "INSERT INTO $table (key, value, value_size_in_KiB) VALUES (?, ?, ?) " +
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
      connection.prepareStatement("DELETE FROM $table WHERE key=?").use {
        it.setString(1, key)
        it.executeUpdate()
      }
    }
  }

  @TestOnly
  override fun flush() {
    pool.connection.use { connection ->
      connection.prepareStatement("TRUNCATE $table").use { it.executeUpdate() }
    }
  }

  private fun initKeyValueTable() {
    pool.connection.use { connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $table (" +
              "id BIGSERIAL PRIMARY KEY," +
              "key VARCHAR(255) NOT NULL UNIQUE," +
              "value BYTEA NOT NULL," +
              "last_update TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP," +
              "value_size_in_KiB INTEGER" +
              ");",
      ).use { it.executeUpdate() }

      connection.prepareStatement(
          "CREATE INDEX IF NOT EXISTS value_size_index ON $table(value_size_in_KiB);",
      ).use { it.executeUpdate() }
    }
  }
}
