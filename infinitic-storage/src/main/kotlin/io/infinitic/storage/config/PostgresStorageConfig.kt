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

import io.infinitic.cache.config.CacheConfig
import io.infinitic.storage.compression.CompressionConfig
import io.infinitic.storage.config.PostgresConfig.Companion.DEFAULT_DATABASE
import io.infinitic.storage.config.PostgresConfig.Companion.DEFAULT_KEY_SET_TABLE
import io.infinitic.storage.config.PostgresConfig.Companion.DEFAULT_KEY_VALUE_TABLE
import io.infinitic.storage.databases.postgres.PostgresKeySetStorage
import io.infinitic.storage.databases.postgres.PostgresKeyValueStorage
import io.infinitic.storage.keySet.KeySetStorage
import io.infinitic.storage.keyValue.KeyValueStorage

data class PostgresStorageConfig(
  internal val postgres: PostgresConfig,
  override var compression: CompressionConfig? = null,
  override var cache: CacheConfig? = null
) : StorageConfig(), PostgresConfigInterface by postgres {

  override val type = "postgres"

  override val dbKeyValue: KeyValueStorage by lazy {
    PostgresKeyValueStorage.from(postgres)
  }

  override val dbKeySet: KeySetStorage by lazy {
    PostgresKeySetStorage.from(postgres)
  }

  companion object {
    @JvmStatic
    fun builder() = PostgresConfigBuilder()
  }

  /**
   * PostgresStorageConfig builder
   */
  class PostgresConfigBuilder: StorageConfigBuilder {
    private var compression: CompressionConfig? = null
    private var cache: CacheConfig? = null
    private var host: String? = null
    private var port: Int? = null
    private var username: String? = null
    private var password: String? = null
    private var database: String = DEFAULT_DATABASE
    private var keySetTable: String = DEFAULT_KEY_SET_TABLE
    private var keyValueTable: String = DEFAULT_KEY_VALUE_TABLE
    private var maximumPoolSize: Int? = null
    private var minimumIdle: Int? = null
    private var idleTimeout: Long? = null
    private var connectionTimeout: Long? = null
    private var maxLifetime: Long? = null

    fun setCompression(compression: CompressionConfig) = apply { this.compression = compression }
    fun setCache(cache: CacheConfig) = apply { this.cache = cache }
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

    override fun build(): PostgresStorageConfig {
      require(host != null) { "${PostgresConfig::host.name}  must not be null" }
      require(port != null) { "${PostgresConfig::port.name}  must not be null" }
      require(username != null) { "${PostgresConfig::username.name}  must not be null" }

      val postgresConfig = PostgresConfig(
          host = host!!,
          port = port!!,
          username = username!!,
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

      return PostgresStorageConfig(
          compression = compression,
          cache = cache,
          postgres = postgresConfig,
      )
    }
  }
}
