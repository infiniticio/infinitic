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
import io.infinitic.common.fixtures.DockerOnly
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorRetryTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorRetryTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage
import io.infinitic.pulsar.admin.InfiniticPulsarAdmin
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import io.infinitic.pulsar.producers.Producer
import io.infinitic.pulsar.producers.ProducerConfig
import io.infinitic.pulsar.resources.PulsarResources
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.bytebuddy.utility.RandomString
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.api.PulsarClient

@EnabledIf(DockerOnly::class)
class PulsarInfiniticProducerTests : StringSpec(
    {
      val pulsarServer = DockerOnly().pulsarServer!!

      val client = InfiniticPulsarClient(
          PulsarClient.builder().serviceUrl(pulsarServer.pulsarBrokerUrl).build(),
      )

      val admin = InfiniticPulsarAdmin(
          PulsarAdmin.builder().serviceHttpUrl(pulsarServer.httpServiceUrl).build(),
      )

      val tenant = RandomString(10).nextString()
      val namespace = RandomString(10).nextString()

      val pulsarConfig = PulsarConfig(
          webServiceUrl = pulsarServer.httpServiceUrl,
          brokerServiceUrl = pulsarServer.pulsarBrokerUrl,
          tenant = tenant,
          namespace = namespace,
      )

      val pulsarResources = PulsarResources(pulsarConfig)

      val pulsarProducer =
          PulsarInfiniticProducer(Producer(client, ProducerConfig()), pulsarResources)

      "publishing to an absent ClientTopic should not throw, should NOT create the topic" {
        val message = TestFactory.random<ClientMessage>()

        // publishing to an absent ClientTopic should not throw
        shouldNotThrowAny { pulsarProducer.internalSendTo(message, ClientTopic) }

        // publishing to an absent ClientTopic should NOT create it
        val topic = with(pulsarResources) { ClientTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldBe null
      }

      "publishing to a deleted ClientTopic should not throw, should NOT create the topic" {
        val message = TestFactory.random<ClientMessage>()
        val topic = with(pulsarResources) { ClientTopic.fullName(message.entity()) }

        // can be isSuccess or isFailure depending on other tests
        admin.createTenant(tenant, null, null)
        admin.createNamespace("$tenant/$namespace", PoliciesConfig())
        // topic creation
        admin.createTopic(topic, false, 3600).isSuccess shouldBe true

        // publishing to an existing ClientTopic should not throw
        shouldNotThrowAny { pulsarProducer.internalSendTo(message, ClientTopic) }

        // topic deletion
        admin.deleteTopic(topic).isSuccess shouldBe true

        // publishing to a used but deleted ClientTopic should not throw
        shouldNotThrowAny { pulsarProducer.internalSendTo(message, ClientTopic) }

        // publishing to a used but deleted ClientTopic should NOT create it
        admin.getTopicInfo(topic).getOrThrow() shouldBe null
      }

      "publishing to an absent WorkflowTagTopic should not throw, should create the topic" {
        val message = TestFactory.random<WorkflowTagEngineMessage>()

        // publishing to an absent ClientTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowTagEngineTopic)
        }

        // publishing to an absent WorkflowTagTopic should create it
        val topic = with(pulsarResources) { WorkflowTagEngineTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent WorkflowCmdTopic should not throw, should create the topic" {
        val message = TestFactory.random<WorkflowStateEngineCmdMessage>()

        // publishing to an absent WorkflowCmdTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowStateCmdTopic)
        }

        // publishing to an absent WorkflowCmdTopic should create it
        val topic = with(pulsarResources) { WorkflowStateCmdTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent WorkflowEngineTopic should not throw, should create the topic" {
        val message = TestFactory.random<WorkflowStateEngineMessage>()

        // publishing to an absent WorkflowEngineTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowStateEngineTopic)
        }

        // publishing to an absent WorkflowEngineTopic should create it
        val topic = with(pulsarResources) { WorkflowStateEngineTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent DelayedWorkflowEngineTopic should not throw, should create the topic" {
        val message = TestFactory.random<WorkflowStateEngineMessage>()

        // publishing to an absent DelayedWorkflowEngineTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowStateTimerTopic, MillisDuration(1))
        }

        // publishing to an absent DelayedWorkflowEngineTopic should create it
        val topic =
            with(pulsarResources) { WorkflowStateTimerTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent WorkflowEventTopic should not throw, should create the topic" {
        val message = TestFactory.random<WorkflowStateEngineEventMessage>()


        // publishing to an absent WorkflowEventsTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowStateEventTopic)
        }

        // publishing to an absent WorkflowEventsTopic should create it
        val topic = with(pulsarResources) { WorkflowStateEventTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent WorkflowTaskExecutorTopic should not throw, should create the topic" {
        val message = TestFactory.random<ExecuteTask>().copy(
            serviceName = WorkflowTask.WORKFLOW_SERVICE_NAME,
            requester = TestFactory.random<WorkflowRequester>(),
        )

        // publishing to an absent WorkflowTaskExecutorTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowExecutorTopic)
        }

        // publishing to an absent WorkflowTaskExecutorTopic should create it
        val topic = with(pulsarResources) { WorkflowExecutorTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent DelayedWorkflowTaskExecutorTopic should not throw, should create the topic" {
        val message = TestFactory.random<ExecuteTask>().copy(
            serviceName = WorkflowTask.WORKFLOW_SERVICE_NAME,
            requester = TestFactory.random<WorkflowRequester>(),
        )

        // publishing to an absent DelayedWorkflowTaskExecutorTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowExecutorRetryTopic, MillisDuration(1))
        }

        // publishing to an absent DelayedWorkflowTaskExecutorTopic should create it
        val topic =
            with(pulsarResources) { WorkflowExecutorRetryTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent WorkflowTaskEventsTopic should not throw, should create the topic" {
        val message = TestFactory.random<TaskStartedEvent>().copy(
            serviceName = WorkflowTask.WORKFLOW_SERVICE_NAME,
            requester = TestFactory.random<WorkflowRequester>(),
        )

        // publishing to an absent WorkflowTaskEventsTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, WorkflowExecutorEventTopic)
        }

        // publishing to an absent WorkflowTaskEventsTopic should create it
        val topic = with(pulsarResources) { WorkflowExecutorEventTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent ServiceExecutorTopic should not throw, should create the topic" {
        val message = TestFactory.random<ServiceExecutorMessage>()

        // publishing to an absent ServiceExecutorTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, ServiceExecutorTopic)
        }

        // publishing to an absent ServiceExecutorTopic should create it
        val topic = with(pulsarResources) { ServiceExecutorTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent DelayedServiceExecutorTopic should not throw, should create the topic" {
        val message = TestFactory.random<ServiceExecutorMessage>()

        // publishing to an absent DelayedServiceExecutorTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(
              message,
              ServiceExecutorRetryTopic,
              MillisDuration(1),
          )
        }

        // publishing to an absent DelayedServiceExecutorTopic should create it
        val topic = with(pulsarResources) { ServiceExecutorRetryTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }

      "publishing to an absent ServiceEventsTopic Topic should not throw, should create the topic" {
        val message = TestFactory.random<ServiceExecutorEventMessage>()

        // publishing to an absent ServiceEventsTopic should not throw
        shouldNotThrowAny {
          pulsarProducer.internalSendTo(message, ServiceExecutorEventTopic)
        }

        // publishing to an absent ServiceEventsTopic should create it
        val topic = with(pulsarResources) { ServiceExecutorEventTopic.fullName(message.entity()) }
        admin.getTopicInfo(topic).getOrThrow() shouldNotBe null
      }
    },
)
