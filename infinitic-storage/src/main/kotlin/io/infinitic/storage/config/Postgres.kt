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

data class Postgres(
  val host: String = "127.0.0.1",
  val port: Int = 5432,
  val user: String = "postgres",
  val password: Secret? = null,
  val database: String = "infinitic",
  val keySetTable: String = "key_set_storage",
  val keyValueTable: String = "key_value_storage",
  val maximumPoolSize: Int? = null,
  val minimumIdle: Int? = null,
  val idleTimeout: Long? = null, // milli seconds
  val connectionTimeout: Long? = null, // milli seconds
  val maxLifetime: Long? = null // milli seconds
) {

  private val jdbcUrl = "jdbc:postgresql://$host:$port/$database"
  private val jdbcUrlDefault = "jdbc:postgresql://$host:$port/postgres"
  private val driverClassName = "org.postgresql.Driver"

  init {
    maximumPoolSize?.let {
      require(it > 0) { "maximumPoolSize must be strictly positive" }
    }
    minimumIdle?.let {
      require(it >= 0) { "minimumIdle must be positive" }
    }
    idleTimeout?.let {
      require(it > 0) { "idleTimeout must be strictly positive" }
    }
    connectionTimeout?.let {
      require(it > 0) { "connectionTimeout must be strictly positive" }
    }
    maxLifetime?.let {
      require(it > 0) { "maxLifetime must be strictly positive" }
    }

    require(keySetTable.isValidTableName()) { "'$keySetTable' is not a valid PostgresSQL table name" }
    require(keyValueTable.isValidTableName()) { "'$keyValueTable' is not a valid PostgresSQL table name" }
  }

  companion object {
    val pools = ConcurrentHashMap<Postgres, HikariDataSource>()
    fun close() {
      pools.keys.forEach { it.close() }
    }
  }

  fun close() {
    pools[this]?.close()
    pools.remove(this)
  }

  fun getPool() = pools.computeIfAbsent(this) {
    // Create the Database if needed
    initDatabase()
    // create pool
    HikariDataSource(hikariConfig)
  }

  private val hikariConfig = HikariConfig().apply {
    val config = this@Postgres
    jdbcUrl = config.jdbcUrl
    driverClassName = config.driverClassName
    username = config.user
    password = config.password?.value
    config.maximumPoolSize?.let { maximumPoolSize = it }
    config.minimumIdle?.let { minimumIdle = it }
    config.idleTimeout?.let { idleTimeout = it }
    config.connectionTimeout?.let { connectionTimeout = it }
    config.maxLifetime?.let { maxLifetime = it }
  }

  private fun HikariDataSource.databaseExists(databaseName: String): Boolean =
      connection.use { it.metaData.catalogs }.use { resultSet ->
        generateSequence {
          if (resultSet.next()) resultSet.getString(1) else null
        }.any { it == databaseName }
      }

  internal fun HikariDataSource.tableExists(tableName: String): Boolean =
      connection.use { connection ->
        connection.metaData.getTables(null, null, tableName, null).use {
          it.next()
        }
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

  private fun getDefaultPool() = HikariDataSource(
      HikariConfig().apply {
        // use a default source
        jdbcUrl = this@Postgres.jdbcUrlDefault
        driverClassName = this@Postgres.driverClassName
        username = this@Postgres.user
        password = this@Postgres.password?.value
      },
  )

  private fun String.isValidTableName(): Boolean {
    // Check length
    // Note that since Postgres uses bytes and Kotlin uses UTF-16 characters,
    // this will not be entirely correct for multi-byte characters.
    if (toByteArray(Charsets.UTF_8).size > 63) {
      return false
    }

    // Check first character
    if (!first().isLetter() && first() != '_') {
      return false
    }

    // Check illegal characters
    if (any { !it.isLetterOrDigit() && it != '_' && it != '$' }) {
      return false
    }

    // Okay if it passed all checks
    return true
  }
}
