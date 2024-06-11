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
package io.infinitic.storage.config

import com.sksamuel.hoplite.Secret
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.ConcurrentHashMap

data class PostgreSQL(
  val host: String = "127.0.0.1",
  val port: Int = 5432,
  val user: String = "postgres",
  val password: Secret? = null,
  val database: String = "infinitic",
  val maximumPoolSize: Int? = null,
  val minimumIdle: Int? = null,
  val idleTimeout: Long? = null, // milli seconds
  val connectionTimeout: Long? = null, // milli seconds
  val maxLifetime: Long? = null // milli seconds
) {
  init {
    maximumPoolSize?.let {
      require(it > 0) { "maximumPoolSize must by strictly positive" }
    }
    minimumIdle?.let {
      require(it > 0) { "minimumIdle must by strictly positive" }
    }
    idleTimeout?.let {
      require(it > 0) { "idleTimeout must by strictly positive" }
    }
    connectionTimeout?.let {
      require(it > 0) { "connectionTimeout must by strictly positive" }
    }
    maxLifetime?.let {
      require(it > 0) { "maxLifetime must by strictly positive" }
    }
  }

  companion object {
    val pools = ConcurrentHashMap<PostgreSQL, HikariDataSource>()
    fun close() {
      pools.keys.forEach { it.close() }
    }
  }

  fun close() {
    pools[this]?.close()
    pools.remove(this)
  }

  fun getPool() =
      pools.computeIfAbsent(this) {
        // Default values for HikariConfig for Postgres.
        HikariDataSource(
            HikariConfig().apply {
              jdbcUrl = "jdbc:postgresql://$host:$port/$database"
              driverClassName = "org.postgresql.Driver"
              username = user
              password = this@PostgreSQL.password?.value
              this@PostgreSQL.maximumPoolSize?.let { maximumPoolSize = it }
              this@PostgreSQL.minimumIdle?.let { minimumIdle = it }
              this@PostgreSQL.idleTimeout?.let { idleTimeout = it }
              this@PostgreSQL.connectionTimeout?.let { connectionTimeout = it }
              this@PostgreSQL.maxLifetime?.let { maxLifetime = it }
            },
        )
      }
}
