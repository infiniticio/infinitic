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

import io.infinitic.common.SendToMonitoringGlobal
import io.infinitic.common.SendToMonitoringPerName
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkers
import io.infinitic.common.SendToWorkflowEngine
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.pulsar.Topic
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.schemas.schemaDefinition
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

    val sendToMonitoringGlobalEngine: SendToMonitoringGlobal = { message: MonitoringGlobalMessage ->
        pulsarMessageBuilder
            .newMessage(
                Topic.MONITORING_GLOBAL.get(),
                AvroSchema.of(schemaDefinition(MonitoringGlobalEnvelope::class))
            )
            .value(MonitoringGlobalEnvelope.from(message))
            .sendAsync()
            .await()

        Unit
    }

    val sendToMonitoringPerNameEngine: SendToMonitoringPerName = { message: MonitoringPerNameEngineMessage ->
        pulsarMessageBuilder
            .newMessage(
                Topic.MONITORING_PER_NAME.get(),
                AvroSchema.of(schemaDefinition(MonitoringPerNameEnvelope::class))
            )
            .key("${message.taskName}")
            .value(MonitoringPerNameEnvelope.from(message))
            .sendAsync()
            .await()

        Unit
    }

    val sendToTaskEngine: SendToTaskEngine = { message: TaskEngineMessage, after: Float ->
        pulsarMessageBuilder
            .newMessage(
                Topic.TASK_ENGINE.get(),
                AvroSchema.of(schemaDefinition(TaskEngineEnvelope::class))
            )
            .key("${message.taskId}")
            .value(TaskEngineEnvelope.from(message))
            .also {
                if (after > 0F) {
                    it.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
                }
            }
            .sendAsync()
            .await()

        Unit
    }

    val sendToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: Float ->
        pulsarMessageBuilder
            .newMessage(
                Topic.WORKFLOW_ENGINE.get(),
                AvroSchema.of(schemaDefinition(WorkflowEngineEnvelope::class))
            )
            .key("${message.workflowId}")
            .value(WorkflowEngineEnvelope.from(message))
            .also {
                if (after > 0F) {
                    it.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
                }
            }
            .sendAsync()
            .await()

        Unit
    }

    val sendToWorkers: SendToWorkers = { message: TaskExecutorMessage ->
        pulsarMessageBuilder
            .newMessage(
                Topic.WORKERS.get("${message.taskName}"),
                AvroSchema.of(schemaDefinition(TaskExecutorEnvelope::class))
            )
            .key("${message.taskName}")
            .value(TaskExecutorEnvelope.from(message))
            .sendAsync()
            .await()

        Unit
    }
}
