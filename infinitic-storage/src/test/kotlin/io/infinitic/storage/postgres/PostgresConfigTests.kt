package io.infinitic.storage.postgres

import com.sksamuel.hoplite.ConfigException
import io.infinitic.storage.config.StorageConfigImpl
import io.infinitic.storage.config.loadConfigFromYaml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class PostgresConfigTests : StringSpec(
    {

      "maximumPoolSize can not be 0" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    maximumPoolSize: 0
     """,
          )
        }
      }

      "minimumIdle can not be negative" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    minimumIdle: -1
     """,
          )
        }
      }

      "idleTimeout can not be 0" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    idleTimeout: 0
     """,
          )
        }
      }

      "connectionTimeout can not be 0" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    connectionTimeout: 0
     """,
          )
        }
      }

      "checking keyValueTable" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    keyValueTable: "0invalid"
     """,
          )
        }
      }

      "checking keySetTable" {
        shouldThrow<ConfigException> {
          loadConfigFromYaml<StorageConfigImpl>(
              """
storage:
  postgres:
    keySetTable: "0invalid"
     """,
          )
        }
      }
    },
)
