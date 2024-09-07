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
package io.infinitic.workers.config

import io.infinitic.storage.config.InMemoryConfig
import io.infinitic.storage.config.InMemoryStorageConfig
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class WorkflowTagEngineConfigTests : StringSpec(
    {
      val storage = InMemoryStorageConfig(inMemory = InMemoryConfig())

      "Can create WorkflowTagEngineConfig through builder with default parameters" {
        val config = shouldNotThrowAny {
          WorkflowTagEngineConfig.builder().build()
        }

        config.shouldBeInstanceOf<WorkflowTagEngineConfig>()
        config.storage shouldBe null
        config.concurrency shouldBe 1
      }

      "Can create WorkflowTagEngineConfig through builder with all parameters" {
        val config = shouldNotThrowAny {
          WorkflowTagEngineConfig.builder()
              .setConcurrency(10)
              .setStorage(storage)
              .build()
        }

        config.shouldBeInstanceOf<WorkflowTagEngineConfig>()
        config.concurrency shouldBe 10
        config.storage shouldBe storage
      }

      "Concurrency must be positive when building WorkflowTagEngineConfig" {
        val e = shouldThrow<IllegalArgumentException> {
          WorkflowTagEngineConfig.builder()
              .setConcurrency(0)
              .build()
        }
        e.message shouldContain "concurrency"
      }

      "Can create WorkflowTagEngineConfig through YAML with all parameters" {
        val config = shouldNotThrowAny {
          WorkflowTagEngineConfig.fromYamlString(
              """
concurrency: 10
storage:
  inMemory:
          """,
          )
        }

        config.shouldBeInstanceOf<WorkflowTagEngineConfig>()
        config.concurrency shouldBe 10
        config.storage shouldBe storage
      }
    },
)
