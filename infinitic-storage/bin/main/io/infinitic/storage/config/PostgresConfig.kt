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
 * Configuration for PostgresSQL database connection.
 *
 * @property host The host address of the PostgresSQL server.
 * @property port The port number on which the PostgresSQL server is listening.
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
 * PostgresConfig("localhost", 5432, "postgres", null)
 * ```
 *
 * ```java
 * PostgresConfig.builder()
 *  .setHost("localhost")
 *  .setPort(5432)
 *  .setUser("postgres")
 *  .setPassword(null)
 *  .build();
 * ```
 */

interface PostgresConfigInterface {
  val host: String
  val port: Int
  val username: String
  val password: String?
  val database: String
  val schema: String
  val keySetTable: String
  val keyValueTable: String
  val maximumPoolSize: Int?
  val minimumIdle: Int?
  val idleTimeout: Long?
  val connectionTimeout: Long?
  val maxLifetime: Long?
}

@Suppress("unused")
data class PostgresConfig(
  override val host: String,
  override val port: Int,
  override val username: String,
  override val password: String? = null,
  override val database: String = DEFAULT_DATABASE,
  override val schema: String = DEFAULT_SCHEMA,
  override val keySetTable: String = DEFAULT_KEY_SET_TABLE,
  override val keyValueTable: String = DEFAULT_KEY_VALUE_TABLE,
  override val maximumPoolSize: Int? = null,
  override val minimumIdle: Int? = null,
  override val idleTimeout: Long? = null, // milli seconds
  override val connectionTimeout: Long? = null, // milli seconds
  override val maxLifetime: Long? = null // milli seconds
) : PostgresConfigInterface {

  private val jdbcUrlBase = "jdbc:postgresql://$host:$port"
  private val jdbcUrl = "$jdbcUrlBase/$database"
  private val driverClassName = "org.postgresql.Driver"

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
      "Invalid value for '${::database.name}': '$database' is not a valid Postgres database name"
    }
    require(database.isValidSchemaName()) {
      "Invalid value for '${::schema.name}': '$schema' is not a valid Postgres database name"
    }
    require(keySetTable.isValidTableName()) {
      "Invalid value for '${::keySetTable.name}': '$keySetTable' is not a valid Postgres table name"
    }
    require(keyValueTable.isValidTableName()) {
      "Invalid value for '${::keyValueTable.name}': '$keyValueTable' is not a valid Postgres table name"
    }
  }

  /**
   * Returns a string representation of the `PostgresConfig` object with an obfuscated password property.
   * The optional properties are included only if they have non-null values.
   */
  override fun toString() =
      "${this::class.java.simpleName}(host='$host', port=$port, username='$username', password='******', " +
          "database=$database, schema=$schema, keySetTable=$keySetTable, keyValueTable=$keyValueTable" +
          (maximumPoolSize?.let { ", maximumPoolSize=$it" } ?: "") +
          (minimumIdle?.let { ", minimumIdle=$it" } ?: "") +
          (idleTimeout?.let { ", idleTimeout=$it" } ?: "") +
          (connectionTimeout?.let { ", connectionTimeout=$it" } ?: "") +
          (maxLifetime?.let { ", maxLifetime=$it" } ?: "") +
          ")"

  companion object {
    private val pools = ConcurrentHashMap<PostgresConfig, HikariDataSource>()

    internal const val DEFAULT_KEY_VALUE_TABLE = "key_value_storage"
    internal const val DEFAULT_KEY_SET_TABLE = "key_set_storage"
    internal const val DEFAULT_DATABASE = "postgres"
    internal const val DEFAULT_SCHEMA = "infinitic"
  }

  fun close() {
    pools[this]?.close()
    pools.remove(this)
  }

  fun getPool(): HikariDataSource = pools.getOrPut(this) {
    // Create the Database if needed
    initDatabase()
    // Create the Schema if needed
    initSchema()
    // create pool
    HikariDataSource(hikariConfig)
  }

  private val hikariConfig by lazy {
    HikariConfig().apply {
      val config = this@PostgresConfig
      jdbcUrl = config.jdbcUrl
      driverClassName = config.driverClassName
      username = config.username
      password = config.password
      schema = config.schema
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
        }.any { it == databaseName }
      }

  internal fun HikariDataSource.tableExists(tableName: String): Boolean =
      connection.use { connection ->
        connection.metaData.getTables(null, null, tableName, null).use {
          it.next()
        }
      }

  private fun initDatabase() {
    getDefaultPool(DEFAULT_DATABASE).use { pool ->
      if (!pool.databaseExists(database)) {
        pool.connection.use { connection ->
          connection.createStatement().use {
            it.executeUpdate("CREATE DATABASE $database")
          }
        }
      }
    }
  }

  private fun HikariDataSource.schemaExists(schemaName: String): Boolean =
      connection.use { conn ->
        conn.metaData.schemas.use { resultSet ->
          generateSequence {
            if (resultSet.next()) resultSet.getString("TABLE_SCHEM") else null
          }.any { it.lowercase() == schemaName.lowercase() }
        }
      }

  private fun initSchema() {
    getDefaultPool(database).use { pool ->
      if (!pool.schemaExists(schema)) {
        pool.connection.use { connection ->
          connection.createStatement().use { statement ->
            statement.executeUpdate("CREATE SCHEMA $schema")
          }
        }
      }
    }
  }

  private fun getDefaultPool(database: String) = HikariDataSource(
      HikariConfig().apply {
        // use a default source
        jdbcUrl = this@PostgresConfig.jdbcUrlBase + "/$database"
        driverClassName = this@PostgresConfig.driverClassName
        username = this@PostgresConfig.username
        password = this@PostgresConfig.password
      },
  )

  private fun String.isValidDatabaseName(): Boolean {
    val regex = "^[a-zA-Z_][a-zA-Z0-9_\$]{0,62}$".toRegex()
    return isNotEmpty() && matches(regex)
  }

  private fun String.isValidSchemaName(): Boolean {
    val regex = "^[a-zA-Z_][a-zA-Z0-9_]{0,62}$".toRegex()
    return isNotEmpty() && matches(regex)
  }

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
