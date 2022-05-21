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

import com.sksamuel.hoplite.Secret
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.ConcurrentHashMap

data class MySQL(
    val host: String = "127.0.0.1",
    val port: Int = 3306,
    val user: String = "root",
    val password: Secret? = null,
    val database: String = "infinitic"
) {
    companion object {
        val pools = ConcurrentHashMap<MySQL, HikariDataSource>()
    }

    fun getPool() = pools.computeIfAbsent(this) {
        HikariDataSource(
            // Default values set according to https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
            HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://$host:$port/$database"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                username = user
                password = this@MySQL.password?.value
                maximumPoolSize = 10
            }
        ).also {
            Runtime.getRuntime().addShutdownHook(Thread { it.close() })
        }
    }
}
