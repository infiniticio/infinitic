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

package io.infinitic.storage.databases.postgres

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
