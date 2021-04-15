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
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineEnvelope
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.getPersistentTopicFullName
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class PulsarConsumerFactory(
    private val pulsarClient: PulsarClient,
    private val pulsarTenant: String,
    private val pulsarNamespace: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CLIENT_RESPONSE_SUBSCRIPTION = "client-response"
        const val TASK_TAG_ENGINE_SUBSCRIPTION = "task-tag-engine"
        const val TASK_ENGINE_SUBSCRIPTION = "task-engine"
        const val WORKFLOW_TAG_ENGINE_SUBSCRIPTION = "workflow-tag-engine"
        const val WORKFLOW_ENGINE_SUBSCRIPTION = "workflow-engine"
        const val TASK_EXECUTOR_SUBSCRIPTION = "task-executor"
        const val METRICS_PER_NAME_SUBSCRIPTION = "metrics-per-name"
        const val METRICS_GLOBAL_SUBSCRIPTION = "metrics-global"
    }

    fun newClientResponseConsumer(consumerName: String, topic: String) =
        newConsumer<ClientEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Exclusive,
            subscriptionName = CLIENT_RESPONSE_SUBSCRIPTION,
            earliest = false
        )

    fun newTaskTagEngineConsumer(consumerName: String, topic: String) =
        newConsumer<TaskTagEngineEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = TASK_TAG_ENGINE_SUBSCRIPTION
        )

    fun newTaskEngineConsumer(consumerName: String, topic: String) =
        newConsumer<TaskEngineEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = TASK_ENGINE_SUBSCRIPTION
        )

    fun newWorkflowTagEngineConsumer(consumerName: String, topic: String) =
        newConsumer<WorkflowTagEngineEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = WORKFLOW_TAG_ENGINE_SUBSCRIPTION
        )

    fun newWorkflowEngineConsumer(consumerName: String, topic: String) =
        newConsumer<WorkflowEngineEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = WORKFLOW_ENGINE_SUBSCRIPTION
        )

    fun newTaskExecutorConsumer(consumerName: String, topic: String) =
        newConsumer<TaskExecutorEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Shared,
            subscriptionName = TASK_EXECUTOR_SUBSCRIPTION
        )

    fun newMetricsPerNameEngineConsumer(consumerName: String, topic: String) =
        newConsumer<MetricsPerNameEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = METRICS_PER_NAME_SUBSCRIPTION
        )

    fun newMetricsGlobalEngineConsumer(consumerName: String, topic: String) =
        newConsumer<MetricsGlobalEnvelope>(
            consumerName = consumerName,
            topic = topic,
            subscriptionType = SubscriptionType.Failover,
            subscriptionName = METRICS_GLOBAL_SUBSCRIPTION
        )

    private inline fun <reified T : Any> newConsumer(
        consumerName: String,
        topic: String,
        subscriptionType: SubscriptionType,
        subscriptionName: String,
        earliest: Boolean = true
    ): Consumer<T> {
        val fullTopic = getPersistentTopicFullName(pulsarTenant, pulsarNamespace, topic)
        logger.info("Topic $fullTopic: creating consumer $consumerName of type ${T::class}")

        return pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<T>()))
            .topic(fullTopic)
            .consumerName(consumerName)
            .negativeAckRedeliveryDelay(30, TimeUnit.SECONDS)
            .subscriptionName(subscriptionName)
            .subscriptionType(subscriptionType)
            .also {
                if (earliest) it.subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            }
            .subscribe()
    }
}
