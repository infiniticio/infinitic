/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.storage.mysql

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.common.storage.keyValue.KeyValueStorage
import org.jetbrains.annotations.TestOnly

private const val MYSQL_TABLE = "key_value_storage"

class MySQLKeyValueStorage(
    private val pool: HikariDataSource
) : KeyValueStorage {

    companion object {
        fun of(config: MySQL) = MySQLKeyValueStorage(config.getPool())
    }

    init {
        // Create MySQL table at init, for first time usage
        pool.connection.use { connection ->
            connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS $MYSQL_TABLE (" +
                    "`id` INT AUTO_INCREMENT PRIMARY KEY," +
                    "`key` VARCHAR(255) NOT NULL UNIQUE," +
                    "`value` BLOB NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8"
            ).use { it.executeUpdate() }
        }
    }

    override suspend fun get(key: String): ByteArray? =
        pool.connection.use {
            it.prepareStatement("SELECT `value` FROM $MYSQL_TABLE WHERE `key`=?")
                ?.use { statement ->
                    statement.setString(1, key)
                    statement.executeQuery()
                        ?.use { result ->
                            if (result.next()) {
                                result.getBytes("value")
                            } else null
                        }
                }
        }

    override suspend fun put(key: String, value: ByteArray) =
        pool.connection.use {
            it.prepareStatement(
                "INSERT INTO $MYSQL_TABLE (`key`, `value`) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE `value`=?"
            )
                ?.use { statement ->
                    statement.setString(1, key)
                    statement.setBytes(2, value)
                    statement.setBytes(3, value)
                    statement.executeUpdate()
                }
            Unit
        }

    override suspend fun del(key: String) =
        pool.connection.use {
            it.prepareStatement("DELETE FROM $MYSQL_TABLE WHERE `key`=?")
                ?.use { statement ->
                    statement.setString(1, key)
                    statement.executeUpdate()
                }
            Unit
        }

    @TestOnly
    override fun flush() {
        pool.connection.use {
            it.prepareStatement("TRUNCATE $MYSQL_TABLE")
                ?.use { statement ->
                    statement.executeUpdate()
                }
        }
    }
}
