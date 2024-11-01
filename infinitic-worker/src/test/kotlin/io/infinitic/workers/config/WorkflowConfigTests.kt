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

import com.sksamuel.hoplite.ConfigException
import io.infinitic.common.transport.config.LoadedBatchConfig
import io.infinitic.workers.samples.WorkflowA
import io.infinitic.workers.samples.WorkflowAImpl
import io.infinitic.workers.samples.WorkflowAImpl_1
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

internal class WorkflowConfigTests :
  StringSpec(
      {
        val workflowName = WorkflowA::class.java.name
        val workflowClass = WorkflowAImpl::class.java.name

        "Can create WorkflowConfig through YAML with an executor" {
          val config = shouldNotThrowAny {
            WorkflowConfig.fromYamlString(
                """
name: $workflowName
executor:
  class: $workflowClass
  batch:
    maxMessages: 100
    maxSeconds: 0.5
          """,
            )
          }

          config.name shouldBe workflowName
          config.executor.shouldBeInstanceOf<WorkflowExecutorConfig>()
          config.executor!!.batchConfig shouldBe LoadedBatchConfig(100, 0.5)
          config.tagEngine shouldBe null
          config.stateEngine shouldBe null
        }

        "Can create WorkflowConfig through YAML with an executor with multiple versions" {
          shouldNotThrowAny {
            WorkflowConfig.fromYamlString(
                """
name: $workflowName
executor:
  classes:
    - $workflowClass
    - ${WorkflowAImpl_1::class.java.name}
          """,
            )
          }
        }

        "Can create WorkflowConfig through YAML with a Tag Engine" {
          val config = shouldNotThrowAny {
            WorkflowConfig.fromYamlString(
                """
name: $workflowName
tagEngine:
          """,
            )
          }

          config.name shouldBe workflowName
          config.executor shouldBe null
          config.tagEngine.shouldBeInstanceOf<WorkflowTagEngineConfig>()
          config.stateEngine shouldBe null
        }

        "Can create WorkflowConfig through YAML with a State Engine" {
          val config = shouldNotThrowAny {
            WorkflowConfig.fromYamlString(
                """
name: $workflowName
stateEngine:
          """,
            )
          }

          config.name shouldBe workflowName
          config.executor shouldBe null
          config.tagEngine shouldBe null
          config.stateEngine.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        }

        "Can create WorkflowConfig through YAML with executor, tag engine and state engine" {
          val config = shouldNotThrowAny {
            WorkflowConfig.fromYamlString(
                """
name: $workflowName
executor:
  class: $workflowClass
tagEngine:
stateEngine:
          """,
            )
          }
          config.name shouldBe workflowName
          config.executor.shouldBeInstanceOf<WorkflowExecutorConfig>()
          config.tagEngine.shouldBeInstanceOf<WorkflowTagEngineConfig>()
          config.stateEngine.shouldBeInstanceOf<WorkflowStateEngineConfig>()
        }

        "class must implements the Workflow" {
          val e = shouldThrow<ConfigException> {
            WorkflowConfig.fromYamlString(
                """
name: UnknownWorkflow
executor:
  class: $workflowClass
          """,
            )
          }
          e.message shouldContain "'$workflowClass' must be an implementation of Workflow 'UnknownWorkflow'"
        }
      },
  )
