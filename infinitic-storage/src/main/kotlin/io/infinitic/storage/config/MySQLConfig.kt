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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.concurrent.ConcurrentHashMap

/**
 * Configuration for MySQL database connection.
 *
 * @property host The host address of the MySQL server.
 * @property port The port number on which the MySQL server is listening.
 * @property username The username for connecting to the database.
 * @property password The password for connecting to the database, if applicable.
 * @property database The name of the database to connect to. Optional, default is "infinitic".
 * @property keySetTable The name of the table used for key sets. Optional, default is "key_set_storage".
 * @property keyValueTable The name of the table used for key-value pairs. Optional, default is "key_value_storage".
 * @property maximumPoolSize The maximum size of the database connection pool. Optional, default is HikariCP driver's default.
 * @property minimumIdle The minimum number of idle connections in the pool. Optional, default is HikariCP driver's default.
 * @property idleTimeout The maximum amount of time a connection is allowed to sit idle in the pool (in milliseconds). Optional, default is HikariCP driver's default.
 * @property connectionTimeout The maximum time that a connection attempt will wait for a connection to be provided (in milliseconds). Optional, default is HikariCP driver's default.
 * @property maxLifetime The maximum lifetime of a connection in the pool (in milliseconds). Optional, default is HikariCP driver's default.
 *
 * Example for local development:
 *
 * ```kotlin
 * MySQLConfig("localhost", 3306, "root", null)
 * ```
 *
 * ```java
 * MySQLConfig.builder()
 *  .setHost("localhost")
 *  .setPort(3306)
 *  .setUser("root")
 *  .setPassword(null)
 *  .build();
 * ```
 */
data class MySQLConfig(
  val host: String,
  val port: Int,
  val username: String,
  val password: String?,
  val database: String = DEFAULT_DATABASE,
  val keySetTable: String = DEFAULT_KEY_SET_TABLE,
  val keyValueTable: String = DEFAULT_KEY_VALUE_TABLE,
  val maximumPoolSize: Int? = null,
  val minimumIdle: Int? = null,
  val idleTimeout: Long? = null, // milli seconds
  val connectionTimeout: Long? = null, // milli seconds
  val maxLifetime: Long? = null // milli seconds
) : DatabaseConfig {
  private val jdbcUrl = "jdbc:mysql://$host:$port/$database"
  private val jdbcUrlDefault = "jdbc:mysql://$host:$port/"
  private val driverClassName = "com.mysql.cj.jdbc.Driver"

  init {
    maximumPoolSize?.let {
      require(it > 0) { "Invalid value for '${::maximumPoolSize.name}': $it. The value must be > 0." }
    }
    minimumIdle?.let {
      require(it >= 0) { "Invalid value for '${::minimumIdle.name}': $it. The value must be >= 0." }
    }
    idleTimeout?.let {
      require(it > 0) { "Invalid value for '${::idleTimeout.name}': $it. The value must be > 0." }
    }
    connectionTimeout?.let {
      require(it > 0) { "Invalid value for '${::connectionTimeout.name}': $it. The value must be > 0." }
    }
    maxLifetime?.let {
      require(it > 0) { "Invalid value for '${::maxLifetime.name}': $it. The value must be > 0." }
    }

    require(database.isValidDatabaseName()) {
      "Invalid value for '${::database.name}': '$database' is not a valid MySQL database name"
    }
    require(keySetTable.isValidTableName()) {
      "Invalid value for '${::keySetTable.name}': '$keySetTable' is not a valid MySQL table name"
    }
    require(keyValueTable.isValidTableName()) {
      "Invalid value for '${::keyValueTable.name}': '$keyValueTable' is not a valid MySQL table name"
    }
  }

  /**
   * Returns a string representation of the `MySQLConfig` object with an obfuscated password property.
   * The optional properties are included only if they have non-null values.
   */
  override fun toString() =
      "${this::class.java.simpleName}(host='$host', port=$port, username='$username', password='******', " +
          "database=$database, keySetTable=$keySetTable, keyValueTable=$keyValueTable" +
          (maximumPoolSize?.let { ", maximumPoolSize=$it" } ?: "") +
          (minimumIdle?.let { ", minimumIdle=$it" } ?: "") +
          (idleTimeout?.let { ", idleTimeout=$it" } ?: "") +
          (connectionTimeout?.let { ", connectionTimeout=$it" } ?: "") +
          (maxLifetime?.let { ", maxLifetime=$it" } ?: "") +
          ")"

  companion object {
    @JvmStatic
    fun builder() = MySQLConfigBuilder()

    private val pools = ConcurrentHashMap<MySQLConfig, HikariDataSource>()
    private const val DEFAULT_KEY_VALUE_TABLE = "key_value_storage"
    private const val DEFAULT_KEY_SET_TABLE = "key_set_storage"
    private const val DEFAULT_DATABASE = "infinitic"
  }

  fun close() {
    pools[this]?.close()
    pools.remove(this)
  }

  fun getPool(): HikariDataSource =
      pools.getOrPut(this) {
        // Create the Database if needed
        initDatabase()
        // create pool
        HikariDataSource(hikariConfig)
      }

  private val hikariConfig by lazy {
    HikariConfig().apply {
      val config = this@MySQLConfig
      jdbcUrl = config.jdbcUrl
      driverClassName = config.driverClassName
      username = config.username
      password = config.password
      config.maximumPoolSize?.let { maximumPoolSize = it }
      config.minimumIdle?.let { minimumIdle = it }
      config.idleTimeout?.let { idleTimeout = it }
      config.connectionTimeout?.let { connectionTimeout = it }
      config.maxLifetime?.let { maxLifetime = it }
    }
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
        username = config.username
        password = config.password
      },
  )

  private fun String.isValidDatabaseName(): Boolean {
    val regex = "^[a-zA-Z_][a-zA-Z0-9_]{0,63}$".toRegex()
    return isNotEmpty() && matches(regex)
  }

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
    private var host: String? = null
    private var port: Int? = null
    private var username: String? = null
    private var password: String? = null
    private var database: String? = null
    private var keySetTable: String? = null
    private var keyValueTable: String? = null
    private var maximumPoolSize: Int? = null
    private var minimumIdle: Int? = null
    private var idleTimeout: Long? = null
    private var connectionTimeout: Long? = null
    private var maxLifetime: Long? = null

    fun setHost(host: String) = apply { this.host = host }
    fun setPort(port: Int) = apply { this.port = port }
    fun setUserName(user: String) = apply { this.username = user }
    fun setPassword(password: String) = apply { this.password = password }
    fun setDatabase(database: String) = apply { this.database = database }
    fun setKeySetTable(keySetTable: String) = apply { this.keySetTable = keySetTable }
    fun setKeyValueTable(keyValueTable: String) = apply { this.keyValueTable = keyValueTable }
    fun setMaximumPoolSize(maximumPoolSize: Int) = apply { this.maximumPoolSize = maximumPoolSize }
    fun setMinimumIdle(minimumIdle: Int) = apply { this.minimumIdle = minimumIdle }
    fun setIdleTimeout(idleTimeout: Long) = apply { this.idleTimeout = idleTimeout }
    fun setConnectionTimeout(connTimeout: Long) = apply { this.connectionTimeout = connTimeout }
    fun setMaxLifetime(maxLifetime: Long) = apply { this.maxLifetime = maxLifetime }

    fun build(): MySQLConfig {
      return MySQLConfig(
          host = host ?: throw IllegalArgumentException("${::host.name} must not be null"),
          port = port ?: throw IllegalArgumentException("${::port.name} must not be null"),
          username = username
            ?: throw IllegalArgumentException("${::username.name} must not be null"),
          password = password,
          database = database ?: DEFAULT_DATABASE,
          keySetTable = keySetTable ?: DEFAULT_KEY_SET_TABLE,
          keyValueTable = keyValueTable ?: DEFAULT_KEY_VALUE_TABLE,
          maximumPoolSize = maximumPoolSize,
          minimumIdle = minimumIdle,
          idleTimeout = idleTimeout,
          connectionTimeout = connectionTimeout,
          maxLifetime = maxLifetime,
      )
    }
  }
}
