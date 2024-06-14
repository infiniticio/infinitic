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
package io.infinitic.storage.databases.mysql

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.storage.config.MySQL
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly

private const val KEY_VALUE_TABLE = "key_value_storage"

class MySQLKeyValueStorage(
  internal val pool: HikariDataSource,
  tablePrefix: String
) : KeyValueStorage {

  companion object {
    fun from(config: MySQL) = MySQLKeyValueStorage(config.getPool(), config.tablePrefix)
  }

  // table's name
  val table =
      (if (tablePrefix.isEmpty()) KEY_VALUE_TABLE else "${tablePrefix}_$KEY_VALUE_TABLE").also {
        if (!it.isValidMySQLTableName()) throw IllegalArgumentException("$it is not a valid MySQL table name")
      }

  init {
    // Create table if needed
    initKeyValueTable()
  }

  override suspend fun get(key: String): ByteArray? =
      pool.connection.use { connection ->
        connection.prepareStatement("SELECT `value` FROM $table WHERE `key`=?")
            .use { statement ->
              statement.setString(1, key)
              statement.executeQuery().use {
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
              "INSERT INTO $table (`key`, `value`) VALUES (?, ?) " +
                  "ON DUPLICATE KEY UPDATE `value`=?",
          )
          .use {
            it.setString(1, key)
            it.setBytes(2, value)
            it.setBytes(3, value)
            it.executeUpdate()
          }
    }
  }

  override suspend fun del(key: String) {
    pool.connection.use { connection ->
      connection.prepareStatement("DELETE FROM $table WHERE `key`=?").use {
        it.setString(1, key)
        it.executeUpdate()
      }
    }
  }

  override fun close() {
    pool.close()
  }

  @TestOnly
  override fun flush() {
    pool.connection.use { connection ->
      connection.prepareStatement("TRUNCATE $table").use { it.executeUpdate() }
    }
  }

  private fun initKeyValueTable() {
    // Here key is typically a workflowId
    // And value is typically a serialized workflow state
    pool.connection.use { connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $table (" +
              "`id` BIGINT(20) AUTO_INCREMENT PRIMARY KEY," +
              "`key` VARCHAR(255) NOT NULL UNIQUE," +
              "`value` LONGBLOB NOT NULL," +
              "`last_update` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
              "`value_size_in_KiB` BIGINT(20) GENERATED ALWAYS AS ((length(`value`) / 1024)) STORED," +
              "KEY `value_size_index` (`value_size_in_KiB`)" +
              ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
      )
          .use { it.executeUpdate() }
    }
  }
}
