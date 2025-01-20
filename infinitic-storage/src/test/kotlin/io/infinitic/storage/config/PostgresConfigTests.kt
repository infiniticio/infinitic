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

import com.sksamuel.hoplite.ConfigException
import io.infinitic.common.config.loadConfigFromYaml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class PostgresConfigTests : StringSpec(
    {
      lateinit var config: PostgresConfig

      beforeEach {
        config = PostgresConfig("localhost", 3306, "root", null)
      }

      "Check PostgresConfig default values do not change to ensure backward compatibility" {
        config.database shouldBe "postgres"
        config.schema shouldBe "infinitic"
        config.keySetTable shouldBe "key_set_storage"
        config.keyValueTable shouldBe "key_value_storage"
      }

      "should throw for invalid database name" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(database = "invalid-name")
        }
        e.message shouldContain "database"
      }

      "should throw for invalid key-set table name" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(keySetTable = "invalid-table")
        }
        e.message shouldContain "keySetTable"
      }

      "should throw for invalid key-value table name" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(keyValueTable = "invalid-table")
        }
        e.message shouldContain "keyValueTable"
      }

      "should throw for invalid maximumPoolSize" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(maximumPoolSize = 0)
        }
        e.message shouldContain "maximumPoolSize"
      }

      "should throw for invalid minimumIdle" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(minimumIdle = -1)
        }
        e.message shouldContain "minimumIdle"
      }

      "should throw for invalid idleTimeout" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(idleTimeout = 0)
        }
        e.message shouldContain "idleTimeout"
      }

      "should throw for invalid connectionTimeout" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(connectionTimeout = 0)
        }
        e.message shouldContain "connectionTimeout"
      }

      "should throw for invalid maxLifetime" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(maxLifetime = 0)
        }
        e.message shouldContain "maxLifetime"
      }

      "toString() should obfuscate password" {
        config.toString() shouldBe "PostgresConfig(host='${config.host}', port=${config.port}, username='${config.username}', password='******', " +
            "database=${config.database}, schema=${config.schema}, keySetTable=${config.keySetTable}, keyValueTable=${config.keyValueTable})"
      }

      "Can not load from yaml with no host" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    host: localhost
    username: root
    """,
          )
        }
      }

      "Can not load from yaml with no port" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    port: 3306
    username: root
    """,
          )
        }
      }

      "Can not load from yaml with no user" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    host: localhost
    port: 3306
    """,
          )
        }
      }

      val yaml = """
storage:
  postgres:
    host: localhost
    port: 3306
    username: root"""

      "can load from yaml with only mandatory parameters" {
        val storageConfig = loadConfigFromYaml<StorageConfigImpl>(yaml)

        storageConfig.storage shouldBe PostgresStorageConfig(
            postgres = PostgresConfig("localhost", 3306, "root", null),
            compression = null,
            cache = null,
        )
      }

      "can load from yaml with optional parameters" {
        val storageConfig = loadConfigFromYaml<StorageConfigImpl>(
            yaml + """
    password: pass
    database: azerty
    schema: infinitic
    keySetTable: keySet
    keyValueTable: keyVal
    maximumPoolSize: 1
    minimumIdle: 2
    idleTimeout: 3
    connectionTimeout: 4
    maxLifetime: 5
            """,
        )

        (storageConfig.storage as PostgresStorageConfig).postgres shouldBe
            PostgresConfig(
                "localhost",
                3306,
                "root",
                "pass",
                "azerty",
                "infinitic",
                "keySet",
                "keyVal",
                1,
                2,
                3,
                4,
                5,
            )
      }
    },
)
