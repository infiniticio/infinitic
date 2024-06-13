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

data class MySQL(
  val host: String = "127.0.0.1",
  val port: Int = 3306,
  val user: String = "root",
  val password: Secret? = null,
  val database: String = "infinitic",
  val tablePrefix: String = "",
  val maxPoolSize: Int? = null
) {

  private val jdbcUrl = "jdbc:mysql://$host:$port/$database"
  private val jdbcUrlDefault = "jdbc:mysql://$host:$port/"
  private val driverClassName = "com.mysql.cj.jdbc.Driver"

  init {
    maxPoolSize?.let {
      require(it > 0) { "maxPoolSize must by strictly positive" }
    }
  }

  companion object {
    val pools = ConcurrentHashMap<MySQL, HikariDataSource>()

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
        // Create the Database if needed
        initDatabase()
        // create pool
        val config = this@MySQL
        HikariDataSource(
            HikariConfig().apply {
              jdbcUrl = config.jdbcUrl
              driverClassName = config.driverClassName
              username = config.user
              password = config.password?.value
              config.maxPoolSize?.let { maximumPoolSize = it }
            },
        )
      }

  private fun initDatabase() {
    getDefaultPool().use { pool ->
      if (!pool.databaseExists(database)) {
        pool.connection.use { connection ->
          connection.createStatement().use {
            it.executeUpdate("CREATE DATABASE $database")
          }
        }
      }
    }
  }

  internal fun HikariDataSource.databaseExists(databaseName: String): Boolean =
      connection.use { it.metaData.catalogs }.use { resultSet ->
        generateSequence {
          if (resultSet.next()) resultSet.getString(1) else null
        }.any { databaseName.equals(it, ignoreCase = true) }
      }

  internal fun HikariDataSource.tableExists(tableName: String): Boolean =
      connection.use { connection ->
        connection.metaData.getTables(null, null, tableName, null).use {
          it.next()
        }
      }

  internal fun getDefaultPool() = HikariDataSource(
      HikariConfig().apply {
        val config = this@MySQL
        // use a default source
        jdbcUrl = config.jdbcUrlDefault
        driverClassName = config.driverClassName
        username = config.user
        password = config.password?.value
      },
  )
}
