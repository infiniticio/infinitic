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

package io.infinitic.pulsar

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.requester.workflowName
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.policies.Policies
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.PulsarResources
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import net.bytebuddy.utility.RandomString
import java.util.concurrent.CompletableFuture

class PulsarInfiniticProducerAsyncTests : StringSpec(
    {
      val tenant = RandomString().nextString()
      val namespace = RandomString().nextString()

      val original = PulsarResources(
          mockk<PulsarInfiniticAdmin>(),
          tenant,
          setOf(),
          namespace,
          setOf(),
          Policies(),
      )

      val pulsarResources = spyk(original) {
        coEvery { initTopicOnce(any(), any(), any()) } returns Result.success(Unit)

        coEvery { initDlqTopicOnce(any(), any(), any()) } returns Result.success(Unit)
      }

      val nameSlot = slot<String>()

      val producer = mockk<Producer> {
        coEvery {
          getUniqueName(capture(nameSlot), null)
        } answers { Result.success(nameSlot.captured) }

        every {
          sendAsync(
              envelope = any<Envelope<out Message>>(),
              after = any<MillisDuration>(),
              topic = any<String>(),
              producerName = any<String>(),
              key = any<String>(),
          )
        } returns CompletableFuture.completedFuture(Unit)
      }

      val infiniticProducerAsync = PulsarInfiniticProducerAsync(producer, pulsarResources)

      "should init client-response topic before sending a message to it" {
        val message = TestFactory.random<ClientMessage>()
        with(infiniticProducerAsync) { message.sendToAsync(ClientTopic) }

        val name = message.recipientName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/response:$name",
              isPartitioned = false,
              isDelayed = false,
          )
        }
      }

      "should init workflow-tag topic before sending a message to it" {
        val message = TestFactory.random<WorkflowTagMessage>()
        with(infiniticProducerAsync) { message.sendToAsync(WorkflowTagTopic) }

        val name = message.workflowName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-tag:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init workflow-cmd topic before sending a message to it" {
        val message = TestFactory.random<WorkflowCmdMessage>()
        with(infiniticProducerAsync) { message.sendToAsync(WorkflowCmdTopic) }

        val name = message.workflowName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-cmd:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init workflow-engine topic before sending a message to it" {
        val message = TestFactory.random<WorkflowEngineMessage>()
        with(infiniticProducerAsync) { message.sendToAsync(WorkflowEngineTopic) }

        val name = message.workflowName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-engine:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init workflow-delay topic before sending a message to it" {
        val message = TestFactory.random<WorkflowEngineMessage>()
        with(infiniticProducerAsync) {
          message.sendToAsync(
              DelayedWorkflowEngineTopic,
              MillisDuration(1),
          )
        }

        val name = message.workflowName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-delay:$name",
              isPartitioned = true,
              isDelayed = true,
          )
        }
      }

      "should init workflow-events topic before sending a message to it" {
        val message = TestFactory.random<WorkflowEventMessage>()
        with(infiniticProducerAsync) { message.sendToAsync(WorkflowEventsTopic) }

        val name = message.workflowName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-events:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init workflow-task-executor topic before sending a message to it" {
        val message = TestFactory.random<ExecuteTask>(
            mapOf(
                "serviceName" to WorkflowTask.SERVICE_NAME,
                "requester" to TestFactory.random<WorkflowRequester>(),
            ),
        )
        with(infiniticProducerAsync) { message.sendToAsync(WorkflowTaskExecutorTopic) }

        val name = message.requester.workflowName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-executor:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init workflow-task-events topic before sending a message to it" {
        val message = TestFactory.random<TaskCompletedEvent>(
            mapOf(
                "serviceName" to WorkflowTask.SERVICE_NAME,
                "requester" to TestFactory.random<WorkflowRequester>(),
            ),
        )
        with(infiniticProducerAsync) { message.sendToAsync(WorkflowTaskEventsTopic) }

        val name = message.requester.workflowName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-events:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init task-tag topic before sending a message to it" {
        val message = TestFactory.random<ServiceTagMessage>()
        with(infiniticProducerAsync) { message.sendToAsync(ServiceTagTopic) }

        val name = message.serviceName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-tag:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init task-executor topic before sending a message to it" {
        val message = TestFactory.random<ExecuteTask>()
        with(infiniticProducerAsync) { message.sendToAsync(ServiceExecutorTopic) }

        val name = message.serviceName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-executor:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }

      "should init task-events topic before sending a message to it" {
        val message = TestFactory.random<TaskCompletedEvent>()
        with(infiniticProducerAsync) { message.sendToAsync(ServiceEventsTopic) }

        val name = message.serviceName.toString()

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-events:$name",
              isPartitioned = true,
              isDelayed = false,
          )
        }
      }
    },
)
