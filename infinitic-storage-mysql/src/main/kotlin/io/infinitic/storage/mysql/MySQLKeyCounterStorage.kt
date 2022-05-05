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
import io.infinitic.common.storage.keyCounter.KeyCounterStorage
import org.jetbrains.annotations.TestOnly

private const val MYSQL_TABLE = "infinitic.key_counter_storage"
class MySQLKeyCounterStorage(
    private val pool: HikariDataSource
) : KeyCounterStorage {

    companion object {
        fun of(config: MySQL) = MySQLKeyCounterStorage(getPool(config))
    }

    init {
        // Init with Database creation IF NOT EXISTS ?
        // CREATE DATABASE IF NOT EXISTS infinitic;
        pool.connection.use{
            it.prepareStatement(
                "CREATE TABLE IF NOT EXISTS $MYSQL_TABLE ("+
                        "`id` INT AUTO_INCREMENT PRIMARY KEY,"+
                        "`key` VARCHAR(255) NOT NULL UNIQUE,"+
                        "`counter` INT DEFAULT 0"+
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8")
            ?.use { statement ->
                statement.executeUpdate()
            }
        }
        Runtime.getRuntime().addShutdownHook(Thread { pool.close() })
    }

    override suspend fun get(key: String): Long =
        pool.connection.use{ connection ->
            connection.prepareStatement("SELECT counter FROM $MYSQL_TABLE WHERE `key`=?")
                ?.use { statement ->
                    statement.setString(1, key)
                    statement.executeQuery()
                        ?.use { result ->
                            if (result.next()) {
                                result.getLong("counter")
                            } else 0L
                        }
                }
        } ?: 0L

    override suspend fun incr(key: String, amount: Long): Unit =
        pool.connection.use{
            it.prepareStatement(
                "INSERT INTO $MYSQL_TABLE (`key`, counter) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE counter=counter+?"
            )?.use { statement ->
                statement.setString(1, key)
                statement.setLong(2, amount)
                statement.setLong(3, amount)
                statement.executeUpdate()
            }
        }

    @TestOnly
    override fun flush() {
        pool.connection.use{
            it.prepareStatement("TRUNCATE $MYSQL_TABLE")
                ?.use { statement ->
                    statement.executeUpdate()
            }
        }
    }
}
