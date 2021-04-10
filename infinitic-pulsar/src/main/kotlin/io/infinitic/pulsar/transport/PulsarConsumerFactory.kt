/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar.transport

import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.metrics.global.messages.MetricsGlobalEnvelope
import io.infinitic.common.metrics.perName.messages.MetricsPerNameEnvelope
import io.infinitic.common.tags.messages.TagEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.ClientResponseTopic
import io.infinitic.pulsar.topics.MetricsGlobalTopic
import io.infinitic.pulsar.topics.MetricsPerNameTopic
import io.infinitic.pulsar.topics.TagEngineCommandsTopic
import io.infinitic.pulsar.topics.TagEngineEventsTopic
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineEventsTopic
import io.infinitic.pulsar.topics.WorkflowExecutorTopic
import io.infinitic.pulsar.topics.getPersistentTopicFullName
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.TimeUnit

class PulsarConsumerFactory(
    private val pulsarClient: PulsarClient,
    private val pulsarTenant: String,
    private val pulsarNamespace: String
) {

    companion object {
        const val CLIENT_RESPONSE_SUBSCRIPTION_NAME = "client-response"
        const val TAG_ENGINE_SUBSCRIPTION_NAME = "tag-engine"
        const val TASK_ENGINE_SUBSCRIPTION_NAME = "task-engine"
        const val WORKFLOW_ENGINE_SUBSCRIPTION_NAME = "workflow-engine"
        const val TASK_EXECUTOR_SUBSCRIPTION = "task-executor"
        const val WORKFLOW_EXECUTOR_SUBSCRIPTION = "workflow-executor"
        const val METRICS_PER_NAME_SUBSCRIPTION = "metrics-per-name"
        const val METRICS_GLOBAL_SUBSCRIPTION = "metrics-global"
    }

    fun newClientResponseConsumer(clientName: String): Consumer<ClientEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<ClientEnvelope>()))
            .topic(
                getPersistentTopicFullName(pulsarTenant, pulsarNamespace, ClientResponseTopic.name(clientName)),
            )
            .consumerName(clientName)
            .negativeAckRedeliveryDelay(5, TimeUnit.SECONDS)
            .subscriptionName(CLIENT_RESPONSE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Exclusive)
            .subscribe()

    fun newTagEngineCommandsConsumer(consumerName: String?, consumerCounter: Int): Consumer<TagEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TagEngineEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TagEngineCommandsTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName-$consumerCounter")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(TAG_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newTagEngineEventsConsumer(consumerName: String?, consumerCounter: Int): Consumer<TagEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TagEngineEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TagEngineEventsTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName-$consumerCounter")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(TAG_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newTaskEngineCommandsConsumer(consumerName: String?, consumerCounter: Int): Consumer<TaskEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskEngineEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineCommandsTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName-$consumerCounter")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(TASK_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newTaskEngineEventsConsumer(consumerName: String?, consumerCounter: Int): Consumer<TaskEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskEngineEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineEventsTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName-$consumerCounter")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(TASK_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newWorkflowEngineCommandsConsumer(consumerName: String?, consumerCounter: Int): Consumer<WorkflowEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<WorkflowEngineEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineCommandsTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName-$consumerCounter")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(WORKFLOW_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newWorkflowEngineEventsConsumer(consumerName: String?, consumerCounter: Int): Consumer<WorkflowEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<WorkflowEngineEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineEventsTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName-$consumerCounter")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(WORKFLOW_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newTaskExecutorConsumer(consumerName: String?, taskName: String): Consumer<TaskExecutorEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskExecutorEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskExecutorTopic.name(taskName)))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(TASK_EXECUTOR_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newWorkflowExecutorConsumer(consumerName: String?, workflowName: String): Consumer<TaskExecutorEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskExecutorEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowExecutorTopic.name(workflowName)))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(WORKFLOW_EXECUTOR_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newMetricsPerNameEngineConsumer(consumerName: String?, consumerCounter: Int): Consumer<MetricsPerNameEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<MetricsPerNameEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MetricsPerNameTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName("$consumerName-$consumerCounter")
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(METRICS_PER_NAME_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()

    fun newMetricsGlobalEngineConsumer(consumerName: String?): Consumer<MetricsGlobalEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<MetricsGlobalEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MetricsGlobalTopic.name))
            .also {
                if (consumerName != null) {
                    it.consumerName(consumerName)
                }
            }
            .negativeAckRedeliveryDelay(10, TimeUnit.SECONDS)
            .subscriptionName(METRICS_GLOBAL_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Failover)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()
}
