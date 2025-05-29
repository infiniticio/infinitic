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

import io.infinitic.storage.config.InMemoryStorageConfig
import io.infinitic.workers.samples.WorkflowA
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class WorkflowStateEngineConfigTests : StringSpec(
    {
      val workflowName = WorkflowA::class.java.name
      val storage = InMemoryStorageConfig.builder().build()

      "Can create WorkflowStateEngineConfig through builder with default parameters" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.builder()
              .setWorkflowName(workflowName)
              .build()
        }

        config.workflowName shouldBe workflowName
        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.storage shouldBe null
        config.concurrency shouldBe 1
        config.commandHandlerConcurrency shouldBe 1
        config.eventHandlerConcurrency shouldBe 1
        config.timerHandlerConcurrency shouldBe 1
      }

      "Can create WorkflowStateEngineConfig through builder with concurrency" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.builder()
              .setWorkflowName(workflowName)
              .setConcurrency(10)
              .setStorage(storage)
              .build()
        }

        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.concurrency shouldBe 10
        config.commandHandlerConcurrency shouldBe 10
        config.eventHandlerConcurrency shouldBe 10
        config.timerHandlerConcurrency shouldBe 10
        config.storage shouldBe storage
      }

      "Can create WorkflowStateEngineConfig through builder with all parameters" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.builder()
              .setWorkflowName(workflowName)
              .setConcurrency(10)
              .setCommandHandlerConcurrency(11)
              .setEventHandlerConcurrency(12)
              .setTimerHandlerConcurrency(13)
              .setStorage(storage)
              .build()
        }

        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.concurrency shouldBe 10
        config.commandHandlerConcurrency shouldBe 11
        config.eventHandlerConcurrency shouldBe 12
        config.timerHandlerConcurrency shouldBe 13
        config.storage shouldBe storage
      }

      "WorkflowName is mandatory when building WorkflowStateEngineConfig through builder" {
        val e = shouldThrow<IllegalArgumentException> {
          WorkflowStateEngineConfig.builder()
              .build()
        }
        e.message shouldContain "workflowName"
      }

      "Can create WorkflowStateEngineConfig through YAML without workflowName" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.fromYamlString(
              """
            concurrency: 10
          """.trimIndent(),
          )
        }

        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.workflowName.isBlank() shouldBe true
        config.concurrency shouldBe 10
        config.commandHandlerConcurrency shouldBe 10
        config.eventHandlerConcurrency shouldBe 10
        config.timerHandlerConcurrency shouldBe 10
        config.storage shouldBe null
      }

      "Can create WorkflowStateEngineConfig through YAML without commands only" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.fromYamlString(
              """
            concurrency: 0
            commandHandlerConcurrency: 10
          """.trimIndent(),
          )
        }

        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.workflowName.isBlank() shouldBe true
        config.concurrency shouldBe 0
        config.commandHandlerConcurrency shouldBe 10
        config.eventHandlerConcurrency shouldBe 0
        config.timerHandlerConcurrency shouldBe 0
        config.storage shouldBe null
      }

      "Can create WorkflowStateEngineConfig through YAML without events only" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.fromYamlString(
              """
            concurrency: 0
            eventHandlerConcurrency: 10
          """.trimIndent(),
          )
        }

        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.workflowName.isBlank() shouldBe true
        config.concurrency shouldBe 0
        config.commandHandlerConcurrency shouldBe 0
        config.eventHandlerConcurrency shouldBe 10
        config.timerHandlerConcurrency shouldBe 0
        config.storage shouldBe null
      }

      "Can create WorkflowStateEngineConfig through YAML without retries only" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.fromYamlString(
              """
            concurrency: 0
            timerHandlerConcurrency: 10
          """.trimIndent(),
          )
        }

        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.workflowName.isBlank() shouldBe true
        config.concurrency shouldBe 0
        config.commandHandlerConcurrency shouldBe 0
        config.eventHandlerConcurrency shouldBe 0
        config.timerHandlerConcurrency shouldBe 10
        config.storage shouldBe null
      }

      "Can create WorkflowStateEngineConfig through YAML with all parameters" {
        val config = shouldNotThrowAny {
          WorkflowStateEngineConfig.fromYamlString(
              """
            concurrency: 10
            storage:
              inMemory:
          """.trimEnd(),
          )
        }

        config.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        config.concurrency shouldBe 10
        config.storage shouldBe storage
      }
    },
)
