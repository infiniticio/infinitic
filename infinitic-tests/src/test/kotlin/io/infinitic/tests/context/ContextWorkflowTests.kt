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
package io.infinitic.tests.context

import io.infinitic.clients.InfiniticClient
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.tasks.executor.TaskExecutor.Companion.DEFAULT_TASK_RETRY
import io.infinitic.tasks.executor.TaskExecutor.Companion.DEFAULT_TASK_TIMEOUT
import io.infinitic.workers.InfiniticWorker
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class ContextWorkflowTests :
    StringSpec({

      // each test should not be longer than 10s
      timeout = 10000

      val worker = autoClose(InfiniticWorker.fromConfigResource("/pulsar.yml"))
      val client = autoClose(InfiniticClient.fromConfigResource("/pulsar.yml"))

      val contextWorkflow =
          client.newWorkflow(
              ContextWorkflow::class.java,
              meta = mapOf("foo" to "bar".toByteArray()),
              tags = setOf("foo", "bar"))

      beforeSpec { worker.startAsync() }

      beforeTest { worker.registry.flush() }

      "get id from context" { contextWorkflow.context1() shouldBe client.lastDeferred!!.id }

      "get tags from context" { contextWorkflow.context2() shouldBe setOf("foo", "bar") }

      "get meta from context" {
        contextWorkflow.context3() shouldBe WorkflowMeta(mapOf("foo" to "bar".toByteArray()))
      }

      "get workflow id from Task" { contextWorkflow.context4() shouldBe client.lastDeferred!!.id }

      "get workflow name from Task" {
        contextWorkflow.context5() shouldBe ContextWorkflow::class.java.name
      }

      "get task tags from Task" { contextWorkflow.context6() shouldBe setOf("foo", "bar") }

      "get task meta from Task" {
        contextWorkflow.context7() shouldBe TaskMeta(mapOf("foo" to "bar".toByteArray()))
      }

      "get task retry from config file" {
        contextWorkflow.context8() shouldBe DEFAULT_TASK_RETRY.copy(maximumRetries = 1)
      }

      "get task timeout from config file" {
        contextWorkflow.context9() shouldBe DEFAULT_TASK_TIMEOUT
      }
    })
