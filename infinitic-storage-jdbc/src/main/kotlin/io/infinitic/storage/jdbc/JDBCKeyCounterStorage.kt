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

package io.infinitic.storage.jdbc

import com.zaxxer.hikari.HikariDataSource
import io.infinitic.common.storage.keyCounter.KeyCounterStorage
import org.jetbrains.annotations.TestOnly

class JDBCKeyCounterStorage(
    private val pool: HikariDataSource
) : KeyCounterStorage {

    companion object {
        fun of(config: JDBC) = JDBCKeyCounterStorage(getPool(config))
    }

    init {
        // Init with Database + Table creation IF NOT EXISTS ?
        // CREATE DATABASE IF NOT EXISTS infinitic;
        // CREATE TABLE IF NOT EXISTS infinitic.key_counter_storage (
        //   `id` INT AUTO_INCREMENT PRIMARY KEY,
        //   `key` VARCHAR(255) NOT NULL UNIQUE,
        //   `counter` INT DEFAULT 0
        //) ENGINE=InnoDB DEFAULT CHARSET=utf8;
        Runtime.getRuntime().addShutdownHook(Thread { pool.close() })
    }

    override suspend fun get(key: String): Long =
        pool.connection.use{
            it.prepareStatement("SELECT counter FROM infinitic.key_counter_storage WHERE key=?")
        }?.let {
            it.setString(1, key)
            it.executeQuery()
        }?.let {
            if (it.next()) {  it.getLong(1) } else 0L
        } ?: 0L

    override suspend fun incr(key: String, amount: Long): Unit =
        pool.connection.use{
            it.prepareStatement(
                "INSERT INTO infinitic.key_counter_storage (key, counter) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE counter=counter+?")
        }?.let {
            it.setString(1, key)
            it.setLong(2, amount)
            it.setLong(2, amount)
            it.execute()
        }.run {}

    @TestOnly
    override fun flush() {
        // TRUNCATE table ?
    }
}
