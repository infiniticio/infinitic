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
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.yaml.YamlPropertySource
import io.infinitic.storage.InMemoryConfig
import io.infinitic.storage.MySQLConfig
import io.infinitic.storage.PostgresConfig
import io.infinitic.storage.RedisConfig
import io.infinitic.storage.StorageConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class StorageConfigTests :
  StringSpec(
      {
        "default storage should be inMemory" {
          val config = loadConfigFromYaml<StorageConfigImpl>("nothing:")

          config shouldBe StorageConfigImpl(storageConfig = StorageConfig(inMemory = InMemoryConfig()))
        }

        "default storage should not be compressed" {
          val config = loadConfigFromYaml<StorageConfigImpl>("nothing:")

          config shouldBe StorageConfigImpl(storageConfig = StorageConfig(compression = null))
        }

        "storage without type should default" {
          val default1 = loadConfigFromYaml<StorageConfigImpl>("nothing:")
          val default2 = loadConfigFromYaml<StorageConfigImpl>("storage:")

          default1 shouldBe default2
        }

        "can choose inMemory storage" {
          val config = loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  inMemory:
     """,
          )

          config shouldBe StorageConfigImpl(storageConfig = StorageConfig(inMemory = InMemoryConfig()))
        }

        "can choose Redis storage" {
          val config = loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  redis:
     """,
          )

          config shouldBe StorageConfigImpl(storageConfig = StorageConfig(redis = RedisConfig()))
        }

        "can choose MySQL storage" {
          val config = loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  mysql:
     """,
          )

          config shouldBe StorageConfigImpl(storageConfig = StorageConfig(mysql = MySQLConfig()))
        }

        "can choose Postgres storage" {
          val config = loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
     """,
          )

          config shouldBe StorageConfigImpl(storageConfig = StorageConfig(postgres = PostgresConfig()))
        }

        "can not have multiple definition in storage" {
          val e = shouldThrow<ConfigException> {
            loadConfigFromYaml<StorageConfigImpl>(
                """
storage:
  redis:
  mysql:
     """,
            )
          }
          e.message shouldContain ("Storage should have only one definition")
        }
      },
  )

internal inline fun <reified T : Any> loadConfigFromYaml(yaml: String): T =
    ConfigLoaderBuilder.default()
        .also { builder -> builder.addSource(YamlPropertySource(yaml)) }
        .build()
        .loadConfigOrThrow()
