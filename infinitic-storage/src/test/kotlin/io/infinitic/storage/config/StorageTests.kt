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
import io.infinitic.storage.DockerOnly
import io.infinitic.storage.config.inMemory.InMemoryKeySetStorage
import io.infinitic.storage.config.inMemory.InMemoryKeyValueStorage
import io.infinitic.storage.config.mysql.MySQLKeySetStorage
import io.infinitic.storage.config.mysql.MySQLKeyValueStorage
import io.infinitic.storage.config.redis.RedisKeySetStorage
import io.infinitic.storage.config.redis.RedisKeyValueStorage
import io.infinitic.storage.keyValue.CompressedKeyValueStorage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.testcontainers.containers.MySQLContainer

class StorageTests :
  StringSpec(
      {
        "default storage should be inMemory" {
          val storage = Storage()

          storage shouldBe Storage(inMemory = InMemory())
        }

        "multiple definition should throw" {
          val e =
              shouldThrow<IllegalArgumentException> {
                Storage(inMemory = InMemory(), redis = Redis())
              }
          e.message shouldContain "Multiple definitions for storage"

          val e2 =
              shouldThrow<IllegalArgumentException> { Storage(mysql = MySQL(), redis = Redis()) }
          e2.message shouldContain "Multiple definitions for storage"
        }

        "properties of InMemory" {
          val config = Storage(inMemory = InMemory("test"), compression = null)

          config.type shouldBe "inMemory"

          // config.keySet should be InMemoryKeySetStorage()
          config.keySet::class shouldBe InMemoryKeySetStorage::class
          (config.keySet as InMemoryKeySetStorage).storage shouldBe
              InMemoryKeySetStorage.from(InMemory("test")).storage

          // config.keyValue should be CompressedKeyValueStorage(InMemoryKeyValueStorage())
          config.keyValue::class shouldBe CompressedKeyValueStorage::class
          (config.keyValue as CompressedKeyValueStorage).storage::class shouldBe
              InMemoryKeyValueStorage::class
          ((config.keyValue as CompressedKeyValueStorage).storage as InMemoryKeyValueStorage)
              .storage shouldBe InMemoryKeyValueStorage.from(InMemory("test")).storage
        }

        "properties of Redis" {
          val config = Storage(redis = Redis(), compression = null)

          config.type shouldBe "redis"

          // config.keySet should be RedisKeySetStorage(pool)
          config.keySet::class shouldBe RedisKeySetStorage::class
          (config.keySet as RedisKeySetStorage).pool shouldBe RedisKeySetStorage.from(Redis()).pool

          // config.keyValue should be CompressedKeyValueStorage(RedisKeyValueStorage(pool))
          config.keyValue::class shouldBe CompressedKeyValueStorage::class
          (config.keyValue as CompressedKeyValueStorage).storage::class shouldBe
              RedisKeyValueStorage::class
          ((config.keyValue as CompressedKeyValueStorage).storage as RedisKeyValueStorage)
              .pool shouldBe RedisKeyValueStorage.from(Redis()).pool
        }

        "properties of MySQL"
            .config(enabledIf = { DockerOnly.shouldRun }) {
              val mysqlServer =
                  MySQLContainer<Nothing>("mysql:5.7")
                      .apply {
                        startupAttempts = 1
                        withUsername("test")
                        withPassword("password")
                        withDatabaseName("infinitic")
                      }
                      .let {
                        it.start()
                        it
                      }

              val mysql =
                  MySQL(
                      host = mysqlServer.host,
                      port = mysqlServer.firstMappedPort,
                      user = mysqlServer.username,
                      password = Secret(mysqlServer.password),
                      database = mysqlServer.databaseName,
                  )

              val config = Storage(mysql = mysql, compression = null)

              config.type shouldBe "mysql"

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
      },
  )
