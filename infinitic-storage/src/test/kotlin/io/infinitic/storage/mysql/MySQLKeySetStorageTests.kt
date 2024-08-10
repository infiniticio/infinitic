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
package io.infinitic.storage.mysql

import com.sksamuel.hoplite.Secret
import io.infinitic.storage.DockerOnly
import io.infinitic.storage.MySQLConfig
import io.infinitic.storage.data.Bytes
import io.infinitic.storage.databases.mysql.MySQLKeySetStorage
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.MySQLContainer

@EnabledIf(DockerOnly::class)
class MySQLKeySetStorageTests :
  StringSpec(
      {
        val mysqlServer = MySQLContainer<Nothing>("mysql:8.3")
            .apply {
              startupAttempts = 1
              withUsername("test")
              withPassword("password")
              withDatabaseName("infinitic")
            }
            .also { it.start() }

        val config = MySQLConfig(
            host = mysqlServer.host,
            port = mysqlServer.firstMappedPort,
            user = mysqlServer.username,
            password = Secret(mysqlServer.password),
            database = mysqlServer.databaseName,
        )

        val storage = MySQLKeySetStorage.from(config)

        afterSpec {
          config.close()
          mysqlServer.stop()
        }

        beforeTest { storage.add("foo", "bar".toByteArray()) }

        afterTest { storage.flush() }

        fun equalsTo(set1: Set<ByteArray>, set2: Set<ByteArray>) =
            set1.map { Bytes(it) }.toSet() == set2.map { Bytes(it) }.toSet()

        "check creation of table (default table)" {
          with(config) { storage.pool.tableExists("key_set_storage") } shouldBe true
        }

        "check creation of table (custom table)" {
          val configWithCustomTable = config.copy(keySetTable = "custom_key_set_table")

          MySQLKeySetStorage.from(configWithCustomTable).use {
            with(configWithCustomTable) { it.pool.tableExists("custom_key_set_table") } shouldBe true
          }
        }

        "get should return an empty set on unknown key" {
          storage.get("unknown") shouldBe setOf()
        }

        "get should return the set on known key" {
          equalsTo(storage.get("foo"), setOf("bar".toByteArray())) shouldBe true
        }

        "add on unknown key should create a new set" {
          storage.add("unknown", "42".toByteArray())

          equalsTo(storage.get("unknown"), setOf("42".toByteArray())) shouldBe true
        }

        "add on known key should add to set" {
          storage.add("foo", "42".toByteArray())

          equalsTo(storage.get("foo"), setOf("42".toByteArray(), "bar".toByteArray())) shouldBe true
        }

        "add known value on known key should do nothing" {
          storage.add("foo", "bar".toByteArray())

          equalsTo(storage.get("foo"), setOf("bar".toByteArray())) shouldBe true
        }

        "remove on unknown key should do nothing" { storage.remove("unknown", "42".toByteArray()) }

        "remove unknown value on known key should do nothing" {
          storage.remove("foo", "42".toByteArray())

          equalsTo(storage.get("foo"), setOf("bar".toByteArray())) shouldBe true
        }

        "remove known value on known key should remove from set" {
          storage.remove("foo", "bar".toByteArray())

          equalsTo(storage.get("foo"), setOf()) shouldBe true
        }
      },
  )
