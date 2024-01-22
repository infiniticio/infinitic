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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceEventsTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.transport.WorkflowCmdTopic
import io.infinitic.common.transport.WorkflowEngineTopic
import io.infinitic.common.transport.WorkflowEventsTopic
import io.infinitic.common.transport.WorkflowTagTopic
import io.infinitic.common.transport.WorkflowTaskEventsTopic
import io.infinitic.common.transport.WorkflowTaskExecutorTopic
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.policies.Policies
import io.infinitic.pulsar.consumers.Consumer
import io.infinitic.pulsar.resources.PulsarResources
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import net.bytebuddy.utility.RandomString
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.CompletableFuture

class PulsarInfiniticConsumerAsyncTests : StringSpec(
    {
      val clientName = ClientName("clientTest")
      val workflowName = WorkflowName("workflowTest")
      val serviceName = ServiceName("serviceTest")

      val handler = slot<suspend (Message, MillisInstant) -> Unit>()
      val handlerDlq = slot<suspend (Message?, Exception) -> Unit>()
      val topic = slot<String>()

      val dlqTopic = slot<String?>()

      val subscriptionName = slot<String>()
      val subscriptionNameDlq = slot<String>()
      val subscriptionType = slot<SubscriptionType>()
      val subscriptionInitialPosition = slot<SubscriptionInitialPosition>()
      val consumerName = slot<String>()
      val concurrency = slot<Int>()

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

      val consumer = mockk<Consumer> {
        coEvery {
          startListening(
              capture(handler),
              capture(handlerDlq),
              any(),
              capture(topic),
              captureNullable(dlqTopic),
              capture(subscriptionName),
              capture(subscriptionNameDlq),
              capture(subscriptionType),
              capture(subscriptionInitialPosition),
              capture(consumerName),
              capture(concurrency),
          )
        } returns CompletableFuture.completedFuture(Unit)
      }

      val infiniticConsumerAsync = PulsarInfiniticConsumerAsync(consumer, pulsarResources, 20.0)

      "should init client-response topic before consuming it" {
        val name = "$clientName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(ClientTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 1,
        )


        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/response:$name",
              false,
              false,
          )
        }
      }

      "should init workflow-tag topic before consuming it" {
        val name = "$workflowName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(WorkflowTagTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-tag:$name",
              true,
              false,
          )
        }
      }

      "should init workflow-cmd topic before consuming it" {
        val name = "$workflowName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(WorkflowCmdTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-cmd:$name",
              true,
              false,
          )
        }
      }

      "should init workflow-engine topic before consuming it" {
        val name = "$workflowName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(WorkflowEngineTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-engine:$name",
              true,
              false,
          )
        }
      }

      "should init workflow-delay topic before consuming it" {
        val name = "$workflowName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(DelayedWorkflowEngineTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-delay:$name",
              true,
              true,
          )
        }
      }

      "should init workflow-events topic before consuming it" {
        val name = "$workflowName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(WorkflowEventsTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-events:$name",
              true,
              false,
          )
        }
      }

      "should init workflow-task-executor topic before consuming it" {
        val name = "$workflowName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(WorkflowTaskExecutorTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-executor:$name",
              true,
              false,
          )
        }
      }

      "should init workflow-task-events topic before consuming it" {
        val name = "$workflowName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(WorkflowTaskEventsTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-events:$name",
              true,
              false,
          )
        }
      }

      "should init task-tag topic before consuming it" {
        val name = "$serviceName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(ServiceTagTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-tag:$name",
              true,
              false,
          )
        }
      }

      "should init task-executor topic before consuming it" {
        val name = "$serviceName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(ServiceExecutorTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-executor:$name",
              true,
              false,
          )
        }
      }

      "should init task-events topic before consuming it" {
        val name = "$serviceName"

        infiniticConsumerAsync.start(
            subscription = MainSubscription(ServiceEventsTopic),
            entity = name,
            handler = { _, _ -> },
            beforeDlq = { _, _ -> },
            concurrency = 10,
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-events:$name",
              true,
              false,
          )
        }
      }
    },
)
