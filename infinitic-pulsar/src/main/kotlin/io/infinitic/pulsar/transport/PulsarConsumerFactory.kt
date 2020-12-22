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

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.pulsar.admin.getPersistentTopicFullName
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import io.infinitic.pulsar.topics.MonitoringPerNameTopic
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineEventsTopic
import io.infinitic.pulsar.topics.WorkflowExecutorTopic
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType

class PulsarConsumerFactory(
    private val pulsarClient: PulsarClient,
    private val pulsarTenant: String,
    private val pulsarNamespace: String
) {

    companion object {
        const val WORKFLOW_ENGINE_SUBSCRIPTION_NAME = "workflow-engine"
        const val TASK_ENGINE_SUBSCRIPTION_NAME = "task-engine"
        const val TASK_EXECUTOR_SUBSCRIPTION = "task-executor"
        const val WORKFLOW_EXECUTOR_SUBSCRIPTION = "workflow-executor"
        const val MONITORING_PER_NAME_SUBSCRIPTION = "monitoring-per-name"
        const val MONITORING_GLOBAL_SUBSCRIPTION = "monitoring-global"
    }

    fun newWorkflowEngineConsumer(workerName: String, consumerCounter: Int): Consumer<WorkflowEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<WorkflowEngineEnvelope>()))
            .topics(
                listOf(
                    getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineCommandsTopic.name),
                    getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineEventsTopic.name)
                )
            )
            .consumerName("$workerName-$consumerCounter")
            .subscriptionName(WORKFLOW_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscribe()

    fun newTaskEngineConsumer(workerName: String, consumerCounter: Int): Consumer<TaskEngineEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskEngineEnvelope>()))
            .topics(
                listOf(
                    getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineCommandsTopic.name),
                    getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineEventsTopic.name)
                )
            )
            .consumerName("$workerName-$consumerCounter")
            .subscriptionName(TASK_ENGINE_SUBSCRIPTION_NAME)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscribe()

    fun newTaskExecutorConsumer(workerName: String, consumerCounter: Int, taskName: String): Consumer<TaskExecutorMessage> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskExecutorMessage>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskExecutorTopic.name(taskName)))
            .consumerName("$workerName-$consumerCounter")
            .subscriptionName(TASK_EXECUTOR_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Shared)
            .subscribe()

    fun newWorkflowExecutorConsumer(workerName: String, consumerCounter: Int, workflowName: String): Consumer<TaskExecutorMessage> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<TaskExecutorMessage>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowExecutorTopic.name(workflowName)))
            .consumerName("$workerName-$consumerCounter")
            .subscriptionName(WORKFLOW_EXECUTOR_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Shared)
            .subscribe()

    fun newMonitoringPerNameEngineConsumer(workerName: String, consumerCounter: Int): Consumer<MonitoringPerNameEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<MonitoringPerNameEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringPerNameTopic.name))
            .consumerName("$workerName-$consumerCounter")
            .subscriptionName(MONITORING_PER_NAME_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Key_Shared)
            .subscribe()

    fun newMonitoringGlobalEngineConsumer(workerName: String): Consumer<MonitoringGlobalEnvelope> =
        pulsarClient.newConsumer(Schema.AVRO(schemaDefinition<MonitoringGlobalEnvelope>()))
            .topic(getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringGlobalTopic.name))
            .consumerName("$workerName")
            .subscriptionName(MONITORING_GLOBAL_SUBSCRIPTION)
            .subscriptionType(SubscriptionType.Failover)
            .subscribe()
}
