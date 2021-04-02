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

import io.infinitic.client.output.FunctionsClientOutput
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.transport.SendToClient
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.global.transport.SendToMonitoringGlobal
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameMessage
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
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
import io.infinitic.monitoring.perName.engine.output.FunctionsMonitoringPerNameOutput
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.topics.ClientResponseTopic
import io.infinitic.pulsar.topics.MonitoringGlobalDeadLettersTopic
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import io.infinitic.pulsar.topics.MonitoringPerNameDeadLettersTopic
import io.infinitic.pulsar.topics.MonitoringPerNameTopic
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
import io.infinitic.tasks.engine.output.FunctionsTaskEngineOutput
import io.infinitic.tasks.executor.transport.TaskExecutorDataOutput
import io.infinitic.workflows.engine.output.FunctionsWorkflowEngineOutput
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PulsarOutputs(
    private val pulsarMessageBuilder: PulsarMessageBuilder,
    private val pulsarTenant: String,
    private val pulsarNamespace: String,
    private val clientName: ClientName
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
        ) = PulsarOutputs(
            PulsarMessageBuilderFromClient(pulsarClient, name),
            pulsarTenant,
            pulsarNamespace,
            ClientName(name)
        )

        /*
        Create a new PulsarTransport from a Pulsar Function Context
         */
        fun from(context: Context) = PulsarOutputs(
            PulsarMessageBuilderFromFunction(context),
            context.tenant,
            context.namespace,
            ClientName("client: unused")
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

    val sendToMonitoringPerName: SendToMonitoringPerName = { message: MonitoringPerNameMessage ->
        logger.debug("sendToMonitoringPerName {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringPerNameTopic.name),
            MonitoringPerNameEnvelope.from(message),
            "${message.taskName}",
            MillisDuration(0)
        )
    }

    val sendToMonitoringGlobal: SendToMonitoringGlobal = { message: MonitoringGlobalMessage ->
        logger.debug("sendToMonitoringGlobal {}", message)
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringGlobalTopic.name),
            MonitoringGlobalEnvelope.from(message),
            null,
            MillisDuration(0)
        )
    }

    val clientOutput = FunctionsClientOutput(
        clientName,
        sendCommandsToTagEngine,
        sendCommandsToTaskEngine,
        sendCommandsToWorkflowEngine
    )

    val taskEngineOutput = FunctionsTaskEngineOutput(
        sendEventsToClient,
        sendEventsToTagEngine,
        sendEventsToTaskEngine,
        sendEventsToWorkflowEngine,
        sendToTaskExecutors,
        sendToMonitoringPerName
    )

    val workflowEngineOutput = FunctionsWorkflowEngineOutput(
        sendEventsToClient,
        sendEventsToTagEngine,
        sendCommandsToTaskEngine,
        sendEventsToWorkflowEngine,
    )

    val monitoringPerNameOutput = FunctionsMonitoringPerNameOutput(
        sendToMonitoringGlobal
    )

    val taskExecutorOutput = TaskExecutorDataOutput(
        sendEventsToTaskEngine
    )

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

    val sendToMonitoringPerNameDeadLetters: SendToMonitoringPerName = { message: MonitoringPerNameMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringPerNameDeadLettersTopic.name),
            MonitoringPerNameEnvelope.from(message),
            "${message.taskName}",
            MillisDuration(0)
        )
        logger.debug("taskName {} - sendToMonitoringPerNameDeadLetters {}", message.taskName, message)
    }

    val sendToMonitoringGlobalDeadLetters: SendToMonitoringGlobal = { message: MonitoringGlobalMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringGlobalDeadLettersTopic.name),
            MonitoringGlobalEnvelope.from(message),
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
