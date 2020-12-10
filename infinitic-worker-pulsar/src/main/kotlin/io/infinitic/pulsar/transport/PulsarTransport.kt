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
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import io.infinitic.pulsar.topics.MonitoringPerNameTopic
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineEventsTopic
import kotlinx.coroutines.future.await
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.TimeUnit

class PulsarTransport(private val pulsarMessageBuilder: PulsarMessageBuilder) {
    companion object {
        /*
        Create a new PulsarTransport from a Pulsar Client
         */
        fun from(client: PulsarClient) = PulsarTransport(PulsarMessageBuilderFromClient(client))

        /*
        Create a new PulsarTransport from a Pulsar Function Context
         */
        fun from(context: Context) = PulsarTransport(PulsarMessageBuilderFromFunction(context))
    }

    val sendToWorkflowEngineCommands: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: Float ->
        sendPulsarMessage(
            WorkflowEngineCommandsTopic.name,
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
    }

    val sendToWorkflowEngineEvents: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: Float ->
        sendPulsarMessage(
            WorkflowEngineEventsTopic.name,
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
    }

    val sendToTaskEngineCommands: SendToTaskEngine = { message: TaskEngineMessage, after: Float ->
        sendPulsarMessage(
            TaskEngineCommandsTopic.name,
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
    }

    val sendToTaskEngineEvents: SendToTaskEngine = { message: TaskEngineMessage, after: Float ->
        sendPulsarMessage(
            TaskEngineEventsTopic.name,
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
    }

    val sendToMonitoringPerNameEngine: SendToMonitoringPerName = { message: MonitoringPerNameEngineMessage ->
        sendPulsarMessage(
            MonitoringPerNameTopic.name,
            MonitoringPerNameEnvelope.from(message),
            "${message.taskName}",
            0F
        )
    }

    val sendToMonitoringGlobalEngine: SendToMonitoringGlobal = { message: MonitoringGlobalMessage ->
        sendPulsarMessage(
            MonitoringGlobalTopic.name,
            MonitoringGlobalEnvelope.from(message),
            null,
            0F
        )
    }

    val sendToExecutors: SendToExecutors = { message: TaskExecutorMessage ->
        sendPulsarMessage(
            TaskExecutorTopic.name("${message.taskName}"),
            TaskExecutorEnvelope.from(message),
            "${message.taskName}",
            0F
        )
    }

    private suspend inline fun <reified T : Any> sendPulsarMessage(topic: String, msg: T, key: String?, after: Float) {
        pulsarMessageBuilder
            .newMessage(
                topic,
                AvroSchema.of(schemaDefinition<T>())
            )
            .value(msg)
            .also {
                if (key != null) {
                    it.key(key)
                }
                if (after > 0F) {
                    it.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
                }
            }
            .sendAsync()
            .await()
    }
}
