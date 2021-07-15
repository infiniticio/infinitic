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

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.messages.Envelope
import io.infinitic.common.metrics.global.messages.MetricsGlobalEnvelope
import io.infinitic.common.metrics.perName.messages.MetricsPerNameEnvelope
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagEngineEnvelope
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineEnvelope
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.GlobalTopic
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.TopicName
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
import mu.KotlinLogging
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.TimeUnit

class PulsarConsumerFactory(
    private val pulsarClient: PulsarClient,
    pulsarTenant: String,
    pulsarNamespace: String
) {
    private val logger = KotlinLogging.logger {}

    private val topicName = TopicName(pulsarTenant, pulsarNamespace)

    companion object {
        const val CLIENT_RESPONSE_SUBSCRIPTION = "client-response"
    }

    fun newClientConsumer(consumerName: String, clientName: ClientName) =
        newConsumer<ClientEnvelope>(
            consumerName = consumerName,
            topic = topicName.of(clientName),
            subscriptionType = SubscriptionType.Exclusive,
            subscriptionName = CLIENT_RESPONSE_SUBSCRIPTION,
            ackTimeout = 10,
            earliest = false
        )

    fun newConsumer(consumerName: String, taskTopic: TaskTopic, taskName: TaskName): Consumer<out Envelope<*>> {
        val topic = topicName.of(taskTopic, "$taskName")
        val subscriptionName = taskTopic.prefix + "_subscription"

        return when (taskTopic) {
            TaskTopic.TAG_NEW, TaskTopic.TAG_EXISTING -> newConsumer<TaskTagEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Key_Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            TaskTopic.ENGINE_NEW, TaskTopic.ENGINE_EXISTING -> newConsumer<TaskEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Key_Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            TaskTopic.DELAYS -> newConsumer<TaskEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            TaskTopic.EXECUTORS -> newConsumer<TaskExecutorEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Shared,
                subscriptionName = subscriptionName,
                ackTimeout = null
            )
            TaskTopic.METRICS -> newConsumer<MetricsPerNameEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Failover,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
        }
    }

    fun newConsumer(consumerName: String, workflowTaskTopic: WorkflowTaskTopic, workflowName: WorkflowName): Consumer<*> {
        val topic = topicName.of(workflowTaskTopic, "$workflowName")
        val subscriptionName = workflowTaskTopic.prefix + "_subscription"

        return when (workflowTaskTopic) {
            WorkflowTaskTopic.TAG_NEW, WorkflowTaskTopic.TAG_EXISTING -> newConsumer<TaskTagEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Key_Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            WorkflowTaskTopic.ENGINE_NEW, WorkflowTaskTopic.ENGINE_EXISTING -> newConsumer<TaskEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Key_Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            WorkflowTaskTopic.DELAYS -> newConsumer<TaskEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            WorkflowTaskTopic.EXECUTORS -> newConsumer<TaskExecutorEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            WorkflowTaskTopic.METRICS -> newConsumer<MetricsPerNameEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Failover,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
        }
    }

    fun newConsumer(consumerName: String, workflowTopic: WorkflowTopic, workflowName: WorkflowName): Consumer<*> {
        val topic = topicName.of(workflowTopic, "$workflowName")
        val subscriptionName = workflowTopic.prefix + "_subscription"

        return when (workflowTopic) {
            WorkflowTopic.TAG_NEW, WorkflowTopic.TAG_EXISTING -> newConsumer<WorkflowTagEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Key_Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            WorkflowTopic.ENGINE_NEW, WorkflowTopic.ENGINE_EXISTING -> newConsumer<WorkflowEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Key_Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            WorkflowTopic.DELAYS -> newConsumer<WorkflowEngineEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Shared,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
            WorkflowTopic.METRICS -> newConsumer<MetricsPerNameEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Failover,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
        }
    }

    fun newConsumer(consumerName: String, globalTopic: GlobalTopic): Consumer<*> {
        val topic = topicName.of(globalTopic)
        val subscriptionName = globalTopic.prefix + "_subscription"

        return when (globalTopic) {
            GlobalTopic.METRICS -> newConsumer<MetricsGlobalEnvelope>(
                consumerName = consumerName,
                topic = topic,
                subscriptionType = SubscriptionType.Failover,
                subscriptionName = subscriptionName,
                ackTimeout = 60
            )
        }
    }

    private inline fun <reified T : Any> newConsumer(
        consumerName: String,
        topic: String,
        subscriptionType: SubscriptionType,
        subscriptionName: String,
        ackTimeout: Long? = null,
        earliest: Boolean = true
    ): Consumer<T> {
        logger.info { "Topic $topic: creating consumer $consumerName of type ${T::class}" }

        return pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<T>()))
            .topic(topic)
            .consumerName(consumerName)
            .negativeAckRedeliveryDelay(30, TimeUnit.SECONDS)
            .subscriptionName(subscriptionName)
            .subscriptionType(subscriptionType)
            .also {
                if (ackTimeout != null) it.ackTimeout(ackTimeout, TimeUnit.SECONDS)
                if (earliest) it.subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            }
            .subscribe()
    }
}
