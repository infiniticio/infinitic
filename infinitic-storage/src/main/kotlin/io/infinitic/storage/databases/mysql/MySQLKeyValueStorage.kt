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
import io.infinitic.storage.config.MySQLConfig
import io.infinitic.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly

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
            "INSERT INTO $tableName (`key`, `value`) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE `value`=?",
        ).use {
          it.setString(1, key)
          it.setBytes(2, bytes)
          it.setBytes(3, bytes)
          it.executeUpdate()
        }
      }
    }
  }

  override suspend fun getSet(keys: Set<String>): Map<String, ByteArray?> {
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

  override suspend fun putSet(bytes: Map<String, ByteArray?>) {
    pool.connection.use { connection ->
      connection.autoCommit = false
      try {
        bytes.forEach { (key, value) ->
          if (value == null) {
            connection.prepareStatement("DELETE FROM $tableName WHERE `key` = ?").use { statement ->
              statement.setString(1, key)
              statement.executeUpdate()
            }
          } else {
            connection.prepareStatement(
                "INSERT INTO $tableName (`key`, `value`) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE `value`=?",
            ).use { statement ->
              statement.setString(1, key)
              statement.setBytes(2, value)
              statement.setBytes(3, value)
              statement.executeUpdate()
            }
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

  override fun close() {
    pool.close()
  }

  @TestOnly
  override fun flush() {
    pool.connection.use { connection ->
      connection.prepareStatement("TRUNCATE $tableName").use { it.executeUpdate() }
    }
  }

  private fun initKeyValueTable() {
    // Here key is typically a workflowId
    // And value is typically a serialized workflow state
    pool.connection.use { connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $tableName (" +
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
