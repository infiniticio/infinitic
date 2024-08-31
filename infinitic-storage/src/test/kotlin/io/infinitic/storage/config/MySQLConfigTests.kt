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

class MySQLConfigTests : StringSpec(
    {
      lateinit var config: MySQLConfig

      beforeEach {
        config = MySQLConfig("localhost", 3306, "root", null)
      }

      "Check MySQLConfig default values do not change to ensure backward compatibility" {
        config.database shouldBe "infinitic"
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
        config.toString() shouldBe "MySQLConfig(host='${config.host}', port=${config.port}, user='${config.user}', password='******', " +
            "database=${config.database}, keySetTable=${config.keySetTable}, keyValueTable=${config.keyValueTable})"
      }

      "Can not load from yaml with no host" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  mysql:
    host: localhost
    user: root
    """,
          )
        }
      }

      "Can not load from yaml with no port" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  mysql:
    port: 3306
    user: root
    """,
          )
        }
      }

      "Can not load from yaml with no user" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  mysql:
    host: localhost
    port: 3306
    """,
          )
        }
      }

      val yaml = """
storage:
  mysql:
    host: localhost
    port: 3306
    user: root"""

      "can load from yaml with only mandatory parameters" {
        val storageConfig = loadConfigFromYaml<StorageConfigImpl>(yaml)

        storageConfig.storage shouldBe MySQLStorageConfig(
            mysql = MySQLConfig("localhost", 3306, "root", null),
            compression = null,
            cache = null,
        )
      }

      "can load from yaml with optional parameters" {
        val storageConfig = loadConfigFromYaml<StorageConfigImpl>(
            yaml + """
    password: pass
    database: azerty
    keySetTable: keySet
    keyValueTable: keyVal
    maximumPoolSize: 1
    minimumIdle: 2
    idleTimeout: 3
    connectionTimeout: 4
    maxLifetime: 5
            """,
        )

        (storageConfig.storage as MySQLStorageConfig).mysql shouldBe
            MySQLConfig(
                "localhost",
                3306,
                "root",
                "pass",
                "azerty",
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

