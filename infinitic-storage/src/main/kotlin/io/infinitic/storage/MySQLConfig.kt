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
package io.infinitic.storage

import com.sksamuel.hoplite.Secret
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.ConcurrentHashMap

data class MySQLConfig(
  val host: String = "127.0.0.1",
  val port: Int = 3306,
  val user: String = "root",
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

  private val jdbcUrl = "jdbc:mysql://$host:$port/$database"
  private val jdbcUrlDefault = "jdbc:mysql://$host:$port/"
  private val driverClassName = "com.mysql.cj.jdbc.Driver"

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

    require(keySetTable.isValidTableName()) { "'$keySetTable' is not a valid MySQL table name" }
    require(keyValueTable.isValidTableName()) { "'$keyValueTable' is not a valid MySQL table name" }
  }

  companion object {
    @JvmStatic
    fun builder() = MySQLConfigBuilder()
    
    private val pools = ConcurrentHashMap<MySQLConfig, HikariDataSource>()
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
        HikariDataSource(hikariConfig)
      }

  private val hikariConfig = HikariConfig().apply {
    val config = this@MySQLConfig
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
        }.any { databaseName.equals(it, ignoreCase = true) }
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
        val config = this@MySQLConfig
        // use a default source
        jdbcUrl = config.jdbcUrlDefault
        driverClassName = config.driverClassName
        username = config.user
        password = config.password?.value
      },
  )

  private fun String.isValidTableName(): Boolean {
    // Check length
    if (length > 64) {
      return false
    }

    // Check first character
    if (!first().isLetter()) {
      return false
    }

    // Check illegal characters
    if (any { !it.isLetterOrDigit() && it != '_' && it != '$' && it != '#' }) {
      return false
    }

    // Okay if it passed all checks
    return true
  }

  /**
   * MySQLConfig builder (Useful for Java user)
   */
  class MySQLConfigBuilder {
    private val default = MySQLConfig()

    private var host = default.host
    private var port = default.port
    private var user = default.user
    private var password = default.password
    private var database = default.database
    private var keySetTable = default.keySetTable
    private var keyValueTable = default.keyValueTable
    private var maximumPoolSize = default.maximumPoolSize
    private var minimumIdle = default.minimumIdle
    private var idleTimeout = default.idleTimeout
    private var connectionTimeout = default.connectionTimeout
    private var maxLifetime = default.maxLifetime

    fun setHost(host: String) = apply { this.host = host }
    fun setPort(port: Int) = apply { this.port = port }
    fun setUser(user: String) = apply { this.user = user }
    fun setPassword(password: Secret?) = apply { this.password = password }
    fun setDatabase(database: String) = apply { this.database = database }
    fun setKeySetTable(keySetTable: String) = apply { this.keySetTable = keySetTable }
    fun setKeyValueTable(keyValueTable: String) = apply { this.keyValueTable = keyValueTable }
    fun setMaximumPoolSize(maximumPoolSize: Int?) = apply { this.maximumPoolSize = maximumPoolSize }
    fun setMinimumIdle(minimumIdle: Int?) = apply { this.minimumIdle = minimumIdle }
    fun setIdleTimeout(idleTimeout: Long?) = apply { this.idleTimeout = idleTimeout }
    fun setConnectionTimeout(connTimeout: Long?) = apply { this.connectionTimeout = connTimeout }
    fun setMaxLifetime(maxLifetime: Long?) = apply { this.maxLifetime = maxLifetime }

    fun build(): MySQLConfig {
      return MySQLConfig(
          host = host,
          port = port,
          user = user,
          password = password,
          database = database,
          keySetTable = keySetTable,
          keyValueTable = keyValueTable,
          maximumPoolSize = maximumPoolSize,
          minimumIdle = minimumIdle,
          idleTimeout = idleTimeout,
          connectionTimeout = connectionTimeout,
          maxLifetime = maxLifetime,
      )
    }
  }
}
