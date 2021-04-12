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
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.metrics.global.messages.MetricsGlobalEnvelope
import io.infinitic.common.metrics.global.messages.MetricsGlobalMessage
import io.infinitic.common.metrics.global.transport.SendToMetricsGlobal
import io.infinitic.common.metrics.perName.messages.MetricsPerNameEnvelope
import io.infinitic.common.metrics.perName.messages.MetricsPerNameMessage
import io.infinitic.common.metrics.perName.transport.SendToMetricsPerName
import io.infinitic.common.tags.messages.TagEngineEnvelope
import io.infinitic.common.tags.messages.TagEngineMessage
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToTaskExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.topics.ClientResponseTopic
import io.infinitic.pulsar.topics.MetricsGlobalDeadLettersTopic
import io.infinitic.pulsar.topics.MetricsGlobalTopic
import io.infinitic.pulsar.topics.MetricsPerNameDeadLettersTopic
import io.infinitic.pulsar.topics.MetricsPerNameTopic
import io.infinitic.pulsar.topics.TagEngineCommandsTopic
import io.infinitic.pulsar.topics.TagEngineDeadLettersTopic
import io.infinitic.pulsar.topics.TagEngineEventsTopic
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineDeadLettersTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.topics.TaskExecutorDeadLettersTopic
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineDeadLettersTopic
import io.infinitic.pulsar.topics.WorkflowEngineEventsTopic
import io.infinitic.pulsar.topics.WorkflowExecutorDeadLettersTopic
import io.infinitic.pulsar.topics.WorkflowExecutorTopic
import io.infinitic.pulsar.topics.getPersistentTopicFullName
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PulsarOutput(
    private val pulsarMessageBuilder: PulsarMessageBuilder,
    private val pulsarTenant: String,
    private val pulsarNamespace: String
) {
    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass)

    companion object {
        /*
        Create a new PulsarTransport from a Pulsar Client
         */
        fun from(
            pulsarClient: PulsarClient,
            pulsarTenant: String,
            pulsarNamespace: String,
            name: String
        ) = PulsarOutput(
            PulsarMessageBuilderFromClient(pulsarClient, name),
            pulsarTenant,
            pulsarNamespace
        )

        /*
        Create a new PulsarTransport from a Pulsar Function Context
         */
        fun from(context: Context) = PulsarOutput(
            PulsarMessageBuilderFromFunction(context),
            context.tenant,
            context.namespace
        )
    }

    val sendEventsToClient: SendToClient = { message: ClientMessage ->
        logger.debug("sendEventsToClient {}", message)
        val clientName = "${message.clientName}"
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, ClientResponseTopic.name(clientName)),
            ClientEnvelope.from(message),
            null,
            MillisDuration(0)
        )
    }

    val sendCommandsToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: MillisDuration ->
        logger.debug("sendCommandsToWorkflowEngine {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineCommandsTopic.name),
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
    }

    val sendEventsToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: MillisDuration ->
        logger.debug("sendEventsToWorkflowEngine {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineEventsTopic.name),
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
    }

    val sendCommandsToTagEngine: SendToTagEngine = { message: TagEngineMessage ->
        logger.debug("sendCommandsToTagEngine {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TagEngineCommandsTopic.name),
            TagEngineEnvelope.from(message),
            "${message.tag}",
            MillisDuration(0)
        )
    }

    val sendEventsToTagEngine: SendToTagEngine = { message: TagEngineMessage ->
        logger.debug("sendEventsToTagEngine {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TagEngineEventsTopic.name),
            TagEngineEnvelope.from(message),
            "${message.tag}",
            MillisDuration(0)
        )
    }

    val sendCommandsToTaskEngine: SendToTaskEngine = { message: TaskEngineMessage, after: MillisDuration ->
        logger.debug("sendCommandsToTaskEngine {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineCommandsTopic.name),
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
    }

    val sendEventsToTaskEngine: SendToTaskEngine = { message: TaskEngineMessage, after: MillisDuration ->
        logger.debug("sendEventsToTaskEngine {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineEventsTopic.name),
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
    }

    val sendToTaskExecutors: SendToTaskExecutors = { message: TaskExecutorMessage ->
        logger.debug("sendToTaskExecutors {}", message)
        val taskName = "${message.taskName}"
        val isWorkflowTask = (taskName == WorkflowTask::class.java.name)
        val (key, topicName) = if (isWorkflowTask) {
            val workflowName = String(message.taskMeta[WorkflowTask.META_WORKFLOW_NAME]!!)
            listOf(workflowName, WorkflowExecutorTopic.name(workflowName))
        } else {
            listOf(taskName, TaskExecutorTopic.name(taskName))
        }

        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, topicName),
            TaskExecutorEnvelope.from(message),
            key,
            MillisDuration(0)
        )
    }

    val sendToMetricsPerName: SendToMetricsPerName = { message: MetricsPerNameMessage ->
        logger.debug("sendToMonitoringPerName {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MetricsPerNameTopic.name),
            MetricsPerNameEnvelope.from(message),
            "${message.taskName}",
            MillisDuration(0)
        )
    }

    val sendToMetricsGlobal: SendToMetricsGlobal = { message: MetricsGlobalMessage ->
        logger.debug("sendToMonitoringGlobal {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MetricsGlobalTopic.name),
            MetricsGlobalEnvelope.from(message),
            null,
            MillisDuration(0)
        )
    }

    val sendToTagEngineDeadLetters: SendToTagEngine = { message: TagEngineMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TagEngineDeadLettersTopic.name),
            TagEngineEnvelope.from(message),
            "${message.tag}",
            MillisDuration(0)
        )
        logger.debug("taskId {} - sendToTagEngineDeadLetters {}", message.tag, message)
    }

    val sendToTaskEngineDeadLetters: SendToTaskEngine = { message: TaskEngineMessage, after: MillisDuration ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineDeadLettersTopic.name),
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
        logger.debug("taskId {} - sendToTaskEngineDeadLetters {}", message.taskId, message)
    }

    val sendToWorkflowEngineDeadLetters: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: MillisDuration ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineDeadLettersTopic.name),
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
        logger.debug("workflowId {} - sendToWorkflowEngineDeadLetters {}", message.workflowId, message)
    }

    val sendToMetricsPerNameDeadLetters: SendToMetricsPerName = { message: MetricsPerNameMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MetricsPerNameDeadLettersTopic.name),
            MetricsPerNameEnvelope.from(message),
            "${message.taskName}",
            MillisDuration(0)
        )
        logger.debug("taskName {} - sendToMonitoringPerNameDeadLetters {}", message.taskName, message)
    }

    val sendToMetricsGlobalDeadLetters: SendToMetricsGlobal = { message: MetricsGlobalMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MetricsGlobalDeadLettersTopic.name),
            MetricsGlobalEnvelope.from(message),
            null,
            MillisDuration(0)
        )
        logger.debug("sendToMonitoringGlobalDeadLetters {}", message)
    }

    val sendToTaskExecutorDeadLetters: SendToTaskExecutors = { message: TaskExecutorMessage ->
        val taskName = "${message.taskName}"
        val topicName = if (taskName == WorkflowTask::class.java.name) {
            WorkflowExecutorDeadLettersTopic.name(String(message.taskMeta[WorkflowTask.META_WORKFLOW_NAME]!!))
        } else {
            TaskExecutorDeadLettersTopic.name(taskName)
        }

        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, topicName),
            message,
            null,
            MillisDuration(0)
        )
        logger.debug("taskName {} - sendToTaskExecutorDeadLetters {}", message.taskName, message)
    }
}
