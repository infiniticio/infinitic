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

import io.infinitic.storage.DockerOnly
import io.infinitic.storage.databases.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.databases.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.databases.mysql.MySQLKeySetStorage
import io.infinitic.storage.databases.mysql.MySQLKeyValueStorage
import io.infinitic.storage.databases.postgres.PostgresKeySetStorage
import io.infinitic.storage.databases.postgres.PostgresKeyValueStorage
import io.infinitic.storage.databases.redis.RedisKeySetStorage
import io.infinitic.storage.databases.redis.RedisKeyValueStorage
import io.infinitic.storage.keyValue.CompressedKeyValueStorage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer

class StorageTests :
  StringSpec(
      {
        "properties of InMemory" {
          val config = InMemoryStorageConfig(
              inMemory = InMemoryConfig(),
              compression = null,
              cache = null,
          )

          // config.keySet should be InMemoryKeySetStorage()
          config.keySet::class shouldBe InMemoryKeySetStorage::class
          (config.keySet as InMemoryKeySetStorage).storage shouldBe
              InMemoryKeySetStorage.from(InMemoryConfig()).storage

          // config.keyValue should be CompressedKeyValueStorage(InMemoryKeyValueStorage())
          config.keyValue::class shouldBe CompressedKeyValueStorage::class
          (config.keyValue as CompressedKeyValueStorage).storage::class shouldBe
              InMemoryKeyValueStorage::class
          ((config.keyValue as CompressedKeyValueStorage).storage as InMemoryKeyValueStorage)
              .storage shouldBe InMemoryKeyValueStorage.from(InMemoryConfig()).storage
        }

        "properties of Redis" {
          val config = RedisStorageConfig(
              redis = RedisConfig(host = "localhost", port = 6379),
              compression = null,
              cache = null,
          )

          // config.keySet should be RedisKeySetStorage(pool)
          config.keySet::class shouldBe RedisKeySetStorage::class
          (config.keySet as RedisKeySetStorage).pool shouldBe RedisKeySetStorage.from(config.redis).pool

          // config.keyValue should be CompressedKeyValueStorage(RedisKeyValueStorage(pool))
          config.keyValue::class shouldBe CompressedKeyValueStorage::class
          (config.keyValue as CompressedKeyValueStorage).storage::class shouldBe
              RedisKeyValueStorage::class
          ((config.keyValue as CompressedKeyValueStorage).storage as RedisKeyValueStorage)
              .pool shouldBe RedisKeyValueStorage.from(config.redis).pool
        }

        "properties of MySQL".config(enabledIf = { DockerOnly.shouldRun }) {
          val mysqlServer = MySQLContainer<Nothing>("mysql:8.3")
              .apply {
                startupAttempts = 1
                withUsername("test")
                withPassword("password")
                withDatabaseName("infinitic")
              }
              .also { it.start() }

          val mysql = MySQLConfig(
              host = mysqlServer.host,
              port = mysqlServer.firstMappedPort,
              username = mysqlServer.username,
              password = mysqlServer.password,
          ).copy(database = mysqlServer.databaseName)

          val config = MySQLStorageConfig(mysql = mysql, compression = null)

          // config.keySet should be MySQLKeySetStorage(pool)
          config.keySet::class shouldBe MySQLKeySetStorage::class
          (config.keySet as MySQLKeySetStorage).pool shouldBe MySQLKeySetStorage.from(mysql).pool

          // config.keyValue should be CompressedKeyValueStorage(MySQLKeyValueStorage(pool))
          config.keyValue::class shouldBe CompressedKeyValueStorage::class
          (config.keyValue as CompressedKeyValueStorage).storage::class shouldBe
              MySQLKeyValueStorage::class
          (((config.keyValue as CompressedKeyValueStorage)).storage as MySQLKeyValueStorage)
              .pool shouldBe MySQLKeyValueStorage.from(mysql).pool

          mysql.close()
          mysqlServer.close()
        }

        "properties of Postgres".config(enabledIf = { DockerOnly.shouldRun }) {
          val postgresServer = PostgreSQLContainer<Nothing>("postgres:16")
              .apply {
                startupAttempts = 1
                withUsername("user")
                withPassword("secret")
                withDatabaseName("infinitic")
              }
              .also { it.start() }

          val postgresConfig = PostgresConfig(
              host = postgresServer.host,
              port = postgresServer.firstMappedPort,
              username = postgresServer.username,
              password = postgresServer.password,
              database = postgresServer.databaseName,
          )

          val config = PostgresStorageConfig(postgres = postgresConfig, compression = null)

          // config.keySet should be PostgresKeySetStorage(pool)
          config.keySet::class shouldBe PostgresKeySetStorage::class
          (config.keySet as PostgresKeySetStorage).pool shouldBe PostgresKeySetStorage.from(
              postgresConfig,
          ).pool

          // config.keyValue should be CompressedKeyValueStorage(PostgresKeyValueStorage(pool))
          config.keyValue::class shouldBe CompressedKeyValueStorage::class
          (config.keyValue as CompressedKeyValueStorage).storage::class shouldBe
              PostgresKeyValueStorage::class
          (((config.keyValue as CompressedKeyValueStorage)).storage as PostgresKeyValueStorage)
              .pool shouldBe PostgresKeyValueStorage.from(postgresConfig).pool

          postgresConfig.close()
          postgresServer.close()
        }
      },
  )
