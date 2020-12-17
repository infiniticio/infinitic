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

import io.infinitic.client.transport.ClientDataOutput
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.global.transport.SendToMonitoringGlobal
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.monitoring.perName.transport.SendToMonitoringPerName
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.tasks.executors.SendToExecutors
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.monitoring.perName.engine.transport.MonitoringPerNameDataOutput
import io.infinitic.pulsar.admin.getPersistentTopicFullName
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import io.infinitic.pulsar.topics.MonitoringPerNameTopic
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineEventsTopic
import io.infinitic.tasks.engine.transport.TaskEngineDataOutput
import io.infinitic.tasks.executor.transport.TaskExecutorDataOutput
import io.infinitic.workflows.engine.transport.WorkflowEngineDataOutput
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context

class PulsarOutputs(
    private val pulsarMessageBuilder: PulsarMessageBuilder,
    private val pulsarTenant: String,
    private val pulsarNamespace: String
) {

    companion object {
        /*
        Create a new PulsarTransport from a Pulsar Client
         */
        fun from(pulsarClient: PulsarClient, pulsarTenant: String, pulsarNamespace: String) =
            PulsarOutputs(PulsarMessageBuilderFromClient(pulsarClient), pulsarTenant, pulsarNamespace)

        /*
        Create a new PulsarTransport from a Pulsar Function Context
         */
        fun from(context: Context) =
            PulsarOutputs(PulsarMessageBuilderFromFunction(context), context.tenant, context.namespace)
    }

    private val sendToWorkflowEngineCommands: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: Float ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineCommandsTopic.name),
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
    }

    private val sendToWorkflowEngineEvents: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: Float ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, WorkflowEngineEventsTopic.name),
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
    }

    private val sendToTaskEngineCommands: SendToTaskEngine = { message: TaskEngineMessage, after: Float ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineCommandsTopic.name),
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
    }

    private val sendToTaskEngineEvents: SendToTaskEngine = { message: TaskEngineMessage, after: Float ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskEngineEventsTopic.name),
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
    }

    private val sendToTaskExecutors: SendToExecutors = { message: TaskExecutorMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, TaskExecutorTopic.name("${message.taskName}")),
            TaskExecutorEnvelope.from(message),
            "${message.taskName}",
            0F
        )
    }

    private val sendToMonitoringPerName: SendToMonitoringPerName = { message: MonitoringPerNameEngineMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringPerNameTopic.name),
            MonitoringPerNameEnvelope.from(message),
            "${message.taskName}",
            0F
        )
    }

    private val sendToMonitoringGlobal: SendToMonitoringGlobal = { message: MonitoringGlobalMessage ->
        pulsarMessageBuilder.sendPulsarMessage(
            getPersistentTopicFullName(pulsarTenant, pulsarNamespace, MonitoringGlobalTopic.name),
            MonitoringGlobalEnvelope.from(message),
            null,
            0F
        )
    }

    val clientOutput = ClientDataOutput(
        sendToWorkflowEngineCommands,
        sendToTaskEngineCommands
    )

    val workflowEngineOutput = WorkflowEngineDataOutput(
        sendToWorkflowEngineEvents,
        sendToTaskEngineCommands
    )

    val taskEngineOutput = TaskEngineDataOutput(
        sendToWorkflowEngineEvents,
        sendToTaskEngineEvents,
        sendToTaskExecutors,
        sendToMonitoringPerName
    )

    val monitoringPerNameOutput = MonitoringPerNameDataOutput(
        sendToMonitoringGlobal
    )

    val taskExecutorOutput = TaskExecutorDataOutput(
        sendToTaskEngineEvents
    )
}
