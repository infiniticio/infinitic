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
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.resources.ClientTopicDescription
import io.infinitic.pulsar.resources.ResourceManager
import io.infinitic.pulsar.resources.ServiceTopicsDescription
import io.infinitic.pulsar.resources.TopicDescription
import io.infinitic.pulsar.resources.WorkflowTopicsDescription
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CompletableFuture

class PulsarInfiniticProducerAsyncTests : StringSpec(
    {
      val nameSlot = slot<String>()
      val descSlot = slot<TopicDescription>()

      val dlqNameSlot = slot<String>()
      val dlqDescSlot = slot<TopicDescription>()

      fun topicName(name: String, desc: TopicDescription) =
          "topic:" + name + ":" + desc.subscriptionName

      fun producerName(name: String, desc: TopicDescription) =
          "producer:" + name + ":" + desc.subscriptionName

      val resourceManager = mockk<ResourceManager> {
        every {
          topicFullName()
        }
        every {
          getTopicName(capture(nameSlot), capture(descSlot))
        } answers { topicName(nameSlot.captured, descSlot.captured) }

        every {
          getDlqTopicName(capture(dlqNameSlot), capture(dlqDescSlot))
        } answers { topicName(dlqNameSlot.captured, dlqDescSlot.captured) }

        every {
          getProducerName(capture(nameSlot), capture(descSlot))
        } answers { producerName(nameSlot.captured, descSlot.captured) }

        every { initTopicOnce(any(), any(), any()) } returns Result.success(Unit)

        every { initDlqTopicOnce(any(), any(), any()) } returns Result.success(Unit)
      }

      val producer = mockk<Producer> {
        every {
          getUniqueName(capture(nameSlot), null)
        } answers { Result.success(nameSlot.captured) }

        every {
          sendAsync(
              message = any<Message>(),
              after = any<MillisDuration>(),
              topic = any<String>(),
              producerName = any<String>(),
              key = any<String>(),
          )
        } returns CompletableFuture.completedFuture(Unit)
      }

      val infiniticProducerAsync = PulsarInfiniticProducerAsync(producer, resourceManager)

      "should init client-response topic before sending a message to it" {
        val message = TestFactory.random<ClientMessage>()
        infiniticProducerAsync.sendToClientAsync(message)

        val name = message.recipientName.toString()
        val desc = ClientTopicDescription.RESPONSE

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), false, false)
        }
      }

      "should init workflow-tag topic before sending a message to it" {
        val message = TestFactory.random<WorkflowTagMessage>()
        infiniticProducerAsync.sendToWorkflowTagAsync(message)

        val name = message.workflowName.toString()
        val desc = WorkflowTopicsDescription.TAG

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init workflow-cmd topic before sending a message to it" {
        val message = TestFactory.random<WorkflowEngineMessage>()
        infiniticProducerAsync.sendToWorkflowCmdAsync(message)

        val name = message.workflowName.toString()
        val desc = WorkflowTopicsDescription.CMD

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init workflow-engine topic before sending a message to it" {
        val message = TestFactory.random<WorkflowEngineMessage>()
        infiniticProducerAsync.sendToWorkflowEngineAsync(message)

        val name = message.workflowName.toString()
        val desc = WorkflowTopicsDescription.ENGINE

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init workflow-delay topic before sending a message to it" {
        val message = TestFactory.random<WorkflowEngineMessage>()
        infiniticProducerAsync.sendToWorkflowEngineAsync(message, MillisDuration(1))

        val name = message.workflowName.toString()
        val desc = WorkflowTopicsDescription.ENGINE_DELAYED

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, true)
        }
      }

      "should init workflow-events topic before sending a message to it" {
        val message = TestFactory.random<WorkflowEventMessage>()
        infiniticProducerAsync.sendToWorkflowEventsAsync(message)

        val name = message.workflowName.toString()
        val desc = WorkflowTopicsDescription.EVENTS

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init workflow-task-executor topic before sending a message to it" {
        val message = TestFactory.random<ExecuteTask>(
            mapOf("serviceName" to ServiceName(WorkflowTask::class.java.name)),
        )
        infiniticProducerAsync.sendToTaskExecutorAsync(message)

        val name = message.workflowName.toString()
        val desc = WorkflowTopicsDescription.TASK_EXECUTOR

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init workflow-task-events topic before sending a message to it" {
        val message = TestFactory.random<TaskCompletedEvent>(
            mapOf("serviceName" to ServiceName(WorkflowTask::class.java.name)),
        )
        infiniticProducerAsync.sendToTaskEventsAsync(message)

        val name = message.workflowName.toString()
        val desc = WorkflowTopicsDescription.TASK_EVENTS

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init task-tag topic before sending a message to it" {
        val message = TestFactory.random<ServiceTagMessage>()
        infiniticProducerAsync.sendToTaskTagAsync(message)

        val name = message.serviceName.toString()
        val desc = ServiceTopicsDescription.TAG

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init task-executor topic before sending a message to it" {
        val message = TestFactory.random<ExecuteTask>()
        infiniticProducerAsync.sendToTaskExecutorAsync(message)

        val name = message.serviceName.toString()
        val desc = ServiceTopicsDescription.EXECUTOR

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init task-events topic before sending a message to it" {
        val message = TestFactory.random<TaskCompletedEvent>()
        infiniticProducerAsync.sendToTaskEventsAsync(message)

        val name = message.serviceName.toString()
        val desc = ServiceTopicsDescription.EVENTS

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }
    },
)
