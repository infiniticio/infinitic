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
import io.infinitic.common.data.Name
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
import io.infinitic.pulsar.topics.TopicNamer
import io.infinitic.pulsar.topics.TopicType
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

class PulsarConsumerFactory(
    private val pulsarClient: PulsarClient,
    pulsarTenant: String,
    pulsarNamespace: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val topics = TopicNamer(pulsarTenant, pulsarNamespace)

    companion object {
        const val CLIENT_RESPONSE_SUBSCRIPTION = "client-response"
        const val TASK_TAG_ENGINE_SUBSCRIPTION = "task-tag-engine"
        const val TASK_DELAY_ENGINE_SUBSCRIPTION = "task-delay-engine"
        const val TASK_ENGINE_SUBSCRIPTION = "task-engine"
        const val WORKFLOW_TASK_ENGINE_SUBSCRIPTION = "workflow-task-engine"
        const val WORKFLOW_TAG_ENGINE_SUBSCRIPTION = "workflow-tag-engine"
        const val WORKFLOW_DELAY_ENGINE_SUBSCRIPTION = "workflow-delay-engine"
        const val WORKFLOW_ENGINE_SUBSCRIPTION = "workflow-engine"
        const val TASK_EXECUTOR_SUBSCRIPTION = "task-executor"
        const val WORKFLOW_EXECUTOR_SUBSCRIPTION = "workflow-executor"
        const val METRICS_PER_NAME_SUBSCRIPTION = "metrics-per-name"
        const val METRICS_GLOBAL_SUBSCRIPTION = "metrics-global"
    }

    fun newClientResponseConsumer(consumerName: String, clientName: ClientName) =
        newConsumer<ClientEnvelope>(
            consumerName = consumerName,
            topic = topics.clientTopic(clientName),
            subscriptionType = SubscriptionType.Exclusive,
            subscriptionName = CLIENT_RESPONSE_SUBSCRIPTION,
            ackTimeout = 10,
            earliest = false
        )

    fun newTaskTagEngineConsumer(consumerName: String, topicType: TopicType, taskName: TaskName) =
        newConsumer<TaskTagEngineEnvelope>(
            consumerName = consumerName,
            topic = topics.tagEngineTopic(topicType, taskName),
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = TASK_TAG_ENGINE_SUBSCRIPTION,
            ackTimeout = 60
        )

    fun newTaskEngineConsumer(consumerName: String, topicType: TopicType, name: Name) =
        newConsumer<TaskEngineEnvelope>(
            consumerName = consumerName,
            topic = topics.taskEngineTopic(topicType, name),
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = when (name) {
                is WorkflowName -> WORKFLOW_TASK_ENGINE_SUBSCRIPTION
                is TaskName -> TASK_ENGINE_SUBSCRIPTION
                else -> throw RuntimeException("Unexpected Name type ${name::class} for $name")
            },
            ackTimeout = 60
        )

    fun newTaskDelayEngineConsumer(consumerName: String, taskName: TaskName) =
        newConsumer<TaskEngineEnvelope>(
            consumerName = consumerName,
            topic = topics.delayEngineTopic(taskName),
            subscriptionType = SubscriptionType.Shared,
            subscriptionName = TASK_DELAY_ENGINE_SUBSCRIPTION,
            ackTimeout = 60
        )

    fun newWorkflowTagEngineConsumer(consumerName: String, topicType: TopicType, workflowName: WorkflowName) =
        newConsumer<WorkflowTagEngineEnvelope>(
            consumerName = consumerName,
            topic = topics.tagEngineTopic(topicType, workflowName),
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = WORKFLOW_TAG_ENGINE_SUBSCRIPTION,
            ackTimeout = 60
        )

    fun newWorkflowEngineConsumer(consumerName: String, topicType: TopicType, workflowName: WorkflowName) =
        newConsumer<WorkflowEngineEnvelope>(
            consumerName = consumerName,
            topic = topics.workflowEngineTopic(topicType, workflowName),
            subscriptionType = SubscriptionType.Key_Shared,
            subscriptionName = WORKFLOW_ENGINE_SUBSCRIPTION,
            ackTimeout = 60
        )

    fun newWorkflowDelayEngineConsumer(consumerName: String, workflowName: WorkflowName) =
        newConsumer<WorkflowEngineEnvelope>(
            consumerName = consumerName,
            topic = topics.delayEngineTopic(workflowName),
            subscriptionType = SubscriptionType.Shared,
            subscriptionName = WORKFLOW_DELAY_ENGINE_SUBSCRIPTION,
            ackTimeout = 60
        )

    fun newExecutorConsumer(consumerName: String, name: Name) =
        newConsumer<TaskExecutorEnvelope>(
            consumerName = consumerName,
            topic = topics.executorTopic(name),
            subscriptionType = SubscriptionType.Shared,
            subscriptionName = when (name) {
                is WorkflowName -> WORKFLOW_EXECUTOR_SUBSCRIPTION
                is TaskName -> TASK_EXECUTOR_SUBSCRIPTION
                else -> throw RuntimeException("Unexpected Name type ${name::class} for $name")
            },
            ackTimeout = when (name) {
                is WorkflowName -> 60
                is TaskName -> null
                else -> throw RuntimeException("Unexpected Name type ${name::class} for $name")
            }
        )

    fun newMetricsPerNameEngineConsumer(consumerName: String, taskName: TaskName) =
        newConsumer<MetricsPerNameEnvelope>(
            consumerName = consumerName,
            topic = topics.metricsTopic(taskName),
            subscriptionType = SubscriptionType.Failover,
            subscriptionName = METRICS_PER_NAME_SUBSCRIPTION,
            ackTimeout = 60
        )

    fun newMetricsGlobalEngineConsumer(consumerName: String) =
        newConsumer<MetricsGlobalEnvelope>(
            consumerName = consumerName,
            topic = topics.globalMetricsTopic(),
            subscriptionType = SubscriptionType.Failover,
            subscriptionName = METRICS_GLOBAL_SUBSCRIPTION,
            ackTimeout = 60
        )

    private inline fun <reified T : Any> newConsumer(
        consumerName: String,
        topic: String,
        subscriptionType: SubscriptionType,
        subscriptionName: String,
        ackTimeout: Long? = null,
        earliest: Boolean = true
    ): Consumer<T> {
        logger.info("Topic $topic: creating consumer $consumerName of type ${T::class}")

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
