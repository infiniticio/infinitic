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
package io.infinitic.storage.storages.mysql

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.storage.config.MySQLConfig
import io.infinitic.storage.keySet.KeySetStorage
import org.jetbrains.annotations.TestOnly

class MySQLKeySetStorage(
  internal val pool: HikariDataSource,
  private val tableName: String
) : KeySetStorage {

  companion object {
    fun from(config: MySQLConfig) = MySQLKeySetStorage(config.getPool(), config.keySetTable)
  }

  init {
    // Create table if needed
    initKeySetTable()
  }

  override suspend fun get(key: String): Set<ByteArray> =
      pool.connection.use { connection ->
        connection.prepareStatement("SELECT `value` FROM $tableName WHERE `key` = ?")
            .use { statement ->
              statement.setString(1, key)
              statement.executeQuery().use {
                val values = mutableSetOf<ByteArray>()
                while (it.next()) {
                  values.add(it.getBytes("value"))
                }
                values
              }
            }
      }

  override suspend fun add(key: String, value: ByteArray) {
    pool.connection.use { connection ->
      connection.prepareStatement("INSERT INTO $tableName (`key`, `value`) VALUES (?, ?)").use {
        it.setString(1, key)
        it.setBytes(2, value)
        it.executeUpdate()
      }
    }
  }

  override suspend fun remove(key: String, value: ByteArray) {
    pool.connection.use { connection ->
      connection.prepareStatement("DELETE FROM $tableName WHERE `key`=? AND `value`=?").use {
        it.setString(1, key)
        it.setBytes(2, value)
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
      connection.prepareStatement("TRUNCATE $tableName").use { it.executeUpdate() }
    }
  }

  private fun initKeySetTable() {
    // Here key is typically a tag
    // And value is typically a workflowId
    pool.connection.use { connection ->
      connection.prepareStatement(
          "CREATE TABLE IF NOT EXISTS $tableName ( " +
              "`id` BIGINT(20) AUTO_INCREMENT PRIMARY KEY," +
              "`key` VARCHAR(255) NOT NULL," +
              "`value` VARCHAR(255) NOT NULL," +
              "KEY(`key`)," + // Non unique index creation for faster search
              "KEY `key_value_idx` (`key`,`value`)" +
              ") ENGINE=InnoDB DEFAULT CHARSET=utf8",
      ).use { it.executeUpdate() }
    }
  }
}
