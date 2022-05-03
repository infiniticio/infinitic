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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

// Renaming of getPools ? because in order to name it getPool I have to create a different type of fun
fun getPools(
    host: String,
    port: Int,
    timeout: Int,
    user: String?,
    passwd: String?,
    database: String,
):HikariDataSource {
   return HikariDataSource(
        HikariConfig().apply {
            jdbcUrl         = "jdbc:mysql://$host:$port/$database?profileSQL=true"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username        = user
            password        = passwd
            maximumPoolSize = 10
            maxLifetime     = timeout.toLong()
        }
    )
}

fun getPool(config: MySQL) = getPools(
    host = config.host,
    port = config.port,
    timeout = config.timeout,
    user = config.user,
    passwd = config.password?.value,
    database = config.database,
)
