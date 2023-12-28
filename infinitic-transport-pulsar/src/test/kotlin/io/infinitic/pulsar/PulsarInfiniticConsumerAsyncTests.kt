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

import io.infinitic.common.data.ClientName
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.ClientTopicDescription
import io.infinitic.pulsar.resources.ResourceManager
import io.infinitic.pulsar.resources.ServiceTopicDescription
import io.infinitic.pulsar.resources.TopicDescription
import io.infinitic.pulsar.resources.WorkflowTopicDescription
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.apache.pulsar.client.api.SubscriptionType

class PulsarInfiniticConsumerAsyncTests : StringSpec(
    {
      val clientName = ClientName("clientTest")
      val workflowName = WorkflowName("workflowTest")
      val serviceName = ServiceName("serviceTest")

      val handler = slot<suspend (Message) -> Unit>()
      val handlerDlq = slot<(suspend (Message, Exception) -> Unit)>()
      val nameSlot = slot<String>()
      val descSlot = slot<TopicDescription>()
      val topic = "topicTest"

      val dlqNameSlot = slot<String>()
      val dlqDescSlot = slot<TopicDescription>()
      val dlqTopic = "dlqTopicTest"

      val subscriptionName = slot<String>()
      val subscriptionNameDlq = slot<String>()
      val subscriptionType = slot<SubscriptionType>()
      val consumerName = slot<String>()
      val concurrency = slot<Int>()

      fun topicName(name: String, desc: TopicDescription) = name + ":" + desc.subscriptionName

      val resourceManager = mockk<ResourceManager> {
        every {
          getTopicName(
              capture(nameSlot),
              capture(descSlot),
          )
        } answers { topicName(nameSlot.captured, descSlot.captured) }
        every {
          getDlqTopicName(
              capture(dlqNameSlot),
              capture(dlqDescSlot),
          )
        } answers { topicName(dlqNameSlot.captured, dlqDescSlot.captured) }
        every { initTopicOnce(any(), any(), any()) } returns Result.success(Unit)
        every { initDlqTopicOnce(any(), any(), any()) } returns Result.success(Unit)
      }


      val consumer = mockk<Consumer> {
        every {
          any<CoroutineScope>().startConsumer(
              capture(handler),
              capture(handlerDlq),
              any(),
              topic,
              dlqTopic,
              capture(subscriptionName),
              capture(subscriptionNameDlq),
              capture(subscriptionType),
              capture(consumerName),
              capture(concurrency),
          )
        } returns Unit
      }

      val infiniticConsumerAsync = PulsarInfiniticConsumerAsync(consumer, resourceManager)

      "should init client-response topic before consuming it" {
        infiniticConsumerAsync.startClientConsumerAsync(
            handler = {}, beforeDlq = null, clientName = clientName,
        )

        val name = "$clientName"
        val desc = ClientTopicDescription.RESPONSE

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), false, false)
        }
      }

      "should init workflow-tag topic before consuming it" {
        infiniticConsumerAsync.startWorkflowTagConsumerAsync(
            handler = {}, beforeDlq = null, workflowName = workflowName, 10,
        )

        val name = "$workflowName"
        val desc = WorkflowTopicDescription.TAG

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init workflow-engine topic before consuming it" {
        infiniticConsumerAsync.startWorkflowEngineConsumerAsync(
            handler = {}, beforeDlq = null, workflowName = workflowName, 10,
        )

        val name = "$workflowName"
        val desc = WorkflowTopicDescription.ENGINE

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init workflow-delay topic before consuming it" {
        infiniticConsumerAsync.startDelayedWorkflowEngineConsumerAsync(
            handler = {}, beforeDlq = null, workflowName = workflowName, 10,
        )

        val name = "$workflowName"
        val desc = WorkflowTopicDescription.ENGINE_DELAYED

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, true)
        }
      }

      "should init workflow-task-executor topic before consuming it" {
        infiniticConsumerAsync.startWorkflowTaskConsumerAsync(
            handler = {}, beforeDlq = null, workflowName = workflowName, 10,
        )

        val name = "$workflowName"
        val desc = WorkflowTopicDescription.EXECUTOR

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init task-tag topic before consuming it" {
        infiniticConsumerAsync.startTaskTagConsumerAsync(
            handler = {}, beforeDlq = null, serviceName = serviceName, 10,
        )

        val name = "$serviceName"
        val desc = ServiceTopicDescription.TAG

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }

      "should init task-executor topic before consuming it" {
        infiniticConsumerAsync.startTaskExecutorConsumerAsync(
            handler = {}, beforeDlq = null, serviceName = serviceName, 10,
        )

        val name = "$serviceName"
        val desc = ServiceTopicDescription.EXECUTOR

        verify {
          resourceManager.getTopicName(name, desc)
          resourceManager.initTopicOnce(topicName(name, desc), true, false)
        }
      }
    },
)
