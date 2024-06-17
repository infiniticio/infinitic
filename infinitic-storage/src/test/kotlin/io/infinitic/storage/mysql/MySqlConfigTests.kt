package io.infinitic.storage.mysql

import com.sksamuel.hoplite.ConfigException
import io.infinitic.storage.config.StorageConfigImpl
import io.infinitic.storage.config.loadConfigFromYaml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class MySqlConfigTests :
  StringSpec(
      {

        "maximumPoolSize can not be 0" {
          shouldThrow<ConfigException> {
            loadConfigFromYaml<StorageConfigImpl>(
                """
storage:
  mysql:
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
  mysql:
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
  mysql:
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
  mysql:
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
  mysql:
    keyValueTable: "_invalid"
     """,
            )
          }
        }

        "checking keySetTable" {
          shouldThrow<ConfigException> {
            loadConfigFromYaml<StorageConfigImpl>(
                """
storage:
  mysql:
    keySetTable: "_invalid"
     """,
            )
          }
        }
      },
  )
