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

class RedisConfigTests : StringSpec(
    {
      lateinit var config: RedisConfig

      beforeEach {
        config = RedisConfig(host = "localhost", port = 6379)
      }

      "Check RedisConfig default values do not change to ensure backward compatibility" {
        config.database shouldBe 0
        config.ssl shouldBe false
      }

      "should throw for invalid host" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(host = " ")
        }
        e.message shouldContain "host"
      }

      "should throw for invalid port" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(port = 0)
        }
        e.message shouldContain "port"
      }

      "should throw for invalid database" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(database = -1)
        }
        e.message shouldContain "database"
      }

      "should throw for invalid timeout" {
        val e = shouldThrow<IllegalArgumentException> {
          config.copy(timeout = 0)
        }
        e.message shouldContain "timeout"
      }

      "toString() should obfuscate password" {
        with(config.copy(username = "admin", password = "<PASSWORD>")) {
          toString() shouldBe "RedisConfig(host='$host', port=$port, username='$username', password='******'" +
              ", database=$database, timeout=$timeout, ssl=$ssl, poolConfig=$poolConfig)"
        }
      }

      "Can not load from yaml with no host" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  redis:
    port: 6379
    """,
          )
        }
      }

      "Can not load from yaml with no port" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  redis:
    host: localhost
    """,
          )
        }
      }

      val yaml = """
storage:
  redis:
    host: localhost
    port: 6379"""

      "can load from yaml with only mandatory parameters" {
        val storageConfig = loadConfigFromYaml<StorageConfigImpl>(yaml)

        storageConfig.storage shouldBe RedisStorageConfig(
            redis = RedisConfig("localhost", 6379),
            cache = null,
            compression = null,
        )
      }

      "can load from yaml with optional parameters" {
        val storageConfig = loadConfigFromYaml<StorageConfigImpl>(
            yaml + """
    username: root
    password: pass
    database: 1
    timeout: 2
    ssl: true
    poolConfig:
      maxTotal: 5
      maxIdle: 6
      minIdle: 7
            """,
        )

        (storageConfig.storage as RedisStorageConfig).redis shouldBe
            RedisConfig(
                "localhost",
                6379,
                "root",
                "pass",
                1,
                2,
                true,
                RedisConfig.PoolConfig(
                    maxTotal = 5,
                    maxIdle = 6,
                    minIdle = 7,
                ),
            )
      }
    },
)
