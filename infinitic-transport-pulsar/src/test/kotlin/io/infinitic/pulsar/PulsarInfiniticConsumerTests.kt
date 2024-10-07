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
import io.infinitic.common.fixtures.runWithContextAndCancel
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import io.infinitic.pulsar.resources.PulsarResources
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import net.bytebuddy.utility.RandomString
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Schema
import java.util.concurrent.CompletableFuture

class PulsarInfiniticConsumerTests : StringSpec(
    {
      val clientName = ClientName("clientTest")
      val workflowName = WorkflowName("workflowTest")
      val serviceName = ServiceName("serviceTest")

      val tenant = RandomString().nextString()
      val namespace = RandomString().nextString()

      val pulsarConfig = PulsarConfig(
          brokerServiceUrl = "pulsar://localhost:6650",
          webServiceUrl = "http://localhost:8080",
          tenant = tenant,
          namespace = namespace,
          allowedClusters = setOf(),
          adminRoles = setOf(),
          policies = PoliciesConfig(),
      )

      val pulsarResources = spyk(PulsarResources(pulsarConfig)) {
        coEvery { initTopicOnce(any(), any(), any()) } returns Result.success(Unit)
        coEvery { initDlqTopicOnce(any(), any(), any()) } returns Result.success(Unit)
      }

      val pulsarConsumer = mockk<Consumer<Envelope<out Message>>> {
        every { receiveAsync() } returns CompletableFuture<org.apache.pulsar.client.api.Message<Envelope<out Message>>>()
      }

      val client = mockk<InfiniticPulsarClient> {
        every { newConsumer(any<Schema<Envelope<out Message>>>(), any(), any()) } returns
            Result.success(pulsarConsumer)
      }

      val infiniticConsumer =
          PulsarInfiniticConsumer(client, pulsarConfig.consumer, pulsarResources)

      "should init client-response topic before consuming it" {
        val name = "$clientName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(ClientTopic),
              entity = name,
              concurrency = 1,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }


        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/response:$name",
              isPartitioned = false,
              isTimer = false,
          )
        }
      }

      "should init workflow-tag topic before consuming it" {
        val name = "$workflowName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(WorkflowTagEngineTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-tag:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-cmd topic before consuming it" {
        val name = "$workflowName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(WorkflowStateCmdTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-cmd:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-engine topic before consuming it" {
        val name = "$workflowName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(WorkflowStateEngineTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-engine:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-delay topic before consuming it" {
        val name = "$workflowName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(WorkflowStateTimerTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-delay:$name",
              isPartitioned = true,
              isTimer = true,
          )
        }
      }

      "should init workflow-events topic before consuming it" {
        val name = "$workflowName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(WorkflowStateEventTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-events:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-task-executor topic before consuming it" {
        val name = "$workflowName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(WorkflowExecutorTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-executor:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-task-events topic before consuming it" {
        val name = "$workflowName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(WorkflowExecutorEventTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-events:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init task-tag topic before consuming it" {
        val name = "$serviceName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(ServiceTagEngineTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-tag:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init task-executor topic before consuming it" {
        val name = "$serviceName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(ServiceExecutorTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-executor:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init task-events topic before consuming it" {
        val name = "$serviceName"

        runWithContextAndCancel {
          infiniticConsumer.start(
              subscription = MainSubscription(ServiceExecutorEventTopic),
              entity = name,
              concurrency = 10,
              process = { _, _ -> },
              beforeDlq = { _, _ -> },
          )
        }

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-events:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }
    },
)
