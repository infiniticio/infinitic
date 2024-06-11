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
package io.infinitic.storage.postgresql

import com.sksamuel.hoplite.Secret
import io.infinitic.storage.DockerOnly
import io.infinitic.storage.config.PostgreSQL
import io.infinitic.storage.databases.postgresql.PostgreSQLKeyValueStorage
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.PostgreSQLContainer

@EnabledIf(DockerOnly::class)
class PostgreSQLKeyValueStorageTests :
  StringSpec(
      {
        val postgresqlServer = PostgreSQLContainer<Nothing>("postgres:11")
            .apply {
              startupAttempts = 1
              withUsername("test")
              withPassword("password")
              withDatabaseName("infinitic")
            }
            .also { it.start() }

        val config = PostgreSQL(
            host = postgresqlServer.host,
            port = postgresqlServer.firstMappedPort,
            user = postgresqlServer.username,
            password = Secret(postgresqlServer.password),
            database = postgresqlServer.databaseName,
        )

        val storage = PostgreSQLKeyValueStorage.from(config)

        afterSpec {
          config.close()
          postgresqlServer.stop()
        }

        beforeTest { storage.put("foo", "bar".toByteArray()) }

        afterTest { storage.flush() }

        "getValue should return null on unknown key" { storage.get("unknown") shouldBe null }

        "getValue should return value" {
          storage.get("foo").contentEquals("bar".toByteArray()) shouldBe true
        }

        "putValue on new key should create value" {
          storage.put("foo2", "bar2".toByteArray())

          storage.get("foo2").contentEquals("bar2".toByteArray()) shouldBe true
        }

        "putValue on existing key should update value" {
          storage.put("foo", "bar2".toByteArray())

          storage.get("foo").contentEquals("bar2".toByteArray()) shouldBe true
        }

        "delValue on unknown key does nothing" { storage.del("unknown") }

        "delValue should delete value" {
          storage.del("foo")

          storage.get("foo") shouldBe null
        }
      },
  )
