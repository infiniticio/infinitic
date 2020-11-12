// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.messaging.pulsar.senders

import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.messages.TaskEngineMessage
import io.infinitic.common.workers.messages.WorkerEnvelope
import io.infinitic.common.workers.messages.WorkerMessage
import io.infinitic.common.workflows.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.messages.WorkflowEngineMessage
import io.infinitic.common.SendToMonitoringGlobal
import io.infinitic.common.SendToMonitoringPerName
import io.infinitic.common.SendToTaskEngine
import io.infinitic.common.SendToWorkers
import io.infinitic.common.SendToWorkflowEngine
import io.infinitic.messaging.pulsar.Topic
import io.infinitic.messaging.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.messaging.pulsar.schemas.schemaDefinition
import org.apache.pulsar.client.impl.schema.AvroSchema
import java.util.concurrent.TimeUnit

fun getSendToMonitoringGlobal(pulsarMessageBuilder: PulsarMessageBuilder): SendToMonitoringGlobal =
    {
        message: MonitoringGlobalMessage ->
        pulsarMessageBuilder
            .newMessage(
                Topic.MONITORING_GLOBAL.get(),
                AvroSchema.of(schemaDefinition(MonitoringGlobalEnvelope::class))
            )
            .value(MonitoringGlobalEnvelope.from(message))
            .send()
        Unit
    }

fun getSendToMonitoringPerName(pulsarMessageBuilder: PulsarMessageBuilder): SendToMonitoringPerName =
    {
        message: MonitoringPerNameEngineMessage ->
        pulsarMessageBuilder
            .newMessage(
                Topic.MONITORING_PER_NAME.get(),
                AvroSchema.of(schemaDefinition(MonitoringPerNameEnvelope::class))
            )
            .value(MonitoringPerNameEnvelope.from(message))
            .send()
        Unit
    }

fun getSendToTaskEngine(pulsarMessageBuilder: PulsarMessageBuilder): SendToTaskEngine =
    {
        message: TaskEngineMessage, after: Float ->
        pulsarMessageBuilder
            .newMessage(
                Topic.TASK_ENGINE.get(),
                AvroSchema.of(schemaDefinition(TaskEngineEnvelope::class))
            )
            .value(TaskEngineEnvelope.from(message))
            .also {
                if (after > 0F) {
                    it.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
                }
            }
            .send()
        Unit
    }

fun getSendToWorkflowEngine(pulsarMessageBuilder: PulsarMessageBuilder): SendToWorkflowEngine =
    {
        message: WorkflowEngineMessage, after: Float ->
        pulsarMessageBuilder
            .newMessage(
                Topic.WORKFLOW_ENGINE.get(),
                AvroSchema.of(schemaDefinition(WorkflowEngineEnvelope::class))
            )
            .value(WorkflowEngineEnvelope.from(message))
            .also {
                if (after > 0F) {
                    it.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
                }
            }
            .send()
        Unit
    }

fun getSendToWorkers(pulsarMessageBuilder: PulsarMessageBuilder): SendToWorkers =
    {
        message: WorkerMessage ->
        pulsarMessageBuilder
            .newMessage(
                Topic.WORKERS.get("${message.taskName}"),
                AvroSchema.of(schemaDefinition(WorkerEnvelope::class))
            )
            .value(WorkerEnvelope.from(message))
            .send()
        Unit
    }
