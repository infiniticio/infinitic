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

package io.infinitic.common.workflows.avro

import io.infinitic.common.json.Json
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.messages.CancelWorkflow
import io.infinitic.common.workflows.messages.ChildWorkflowCanceled
import io.infinitic.common.workflows.messages.ChildWorkflowCompleted
import io.infinitic.common.workflows.messages.WorkflowTaskCompleted
import io.infinitic.common.workflows.messages.WorkflowTaskDispatched
import io.infinitic.common.workflows.messages.TimerCompleted
import io.infinitic.common.workflows.messages.DispatchWorkflow
import io.infinitic.common.workflows.messages.ObjectReceived
import io.infinitic.common.workflows.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.messages.TaskCanceled
import io.infinitic.common.workflows.messages.TaskCompleted
import io.infinitic.common.workflows.messages.TaskDispatched
import io.infinitic.common.workflows.messages.WorkflowCanceled
import io.infinitic.common.workflows.messages.WorkflowCompleted
import io.infinitic.common.workflows.state.WorkflowState
import io.infinitic.avro.workflowManager.data.methodRuns.AvroMethodRun
import io.infinitic.avro.workflowManager.messages.AvroCancelWorkflow
import io.infinitic.avro.workflowManager.messages.AvroChildWorkflowCanceled
import io.infinitic.avro.workflowManager.messages.AvroChildWorkflowCompleted
import io.infinitic.avro.workflowManager.messages.AvroWorkflowTaskCompleted
import io.infinitic.avro.workflowManager.messages.AvroWorkflowTaskDispatched
import io.infinitic.avro.workflowManager.messages.AvroDelayCompleted
import io.infinitic.avro.workflowManager.messages.AvroDispatchWorkflow
import io.infinitic.avro.workflowManager.messages.AvroEventReceived
import io.infinitic.avro.workflowManager.messages.AvroTaskCanceled
import io.infinitic.avro.workflowManager.messages.AvroTaskCompleted
import io.infinitic.avro.workflowManager.messages.AvroTaskDispatched
import io.infinitic.avro.workflowManager.messages.AvroWorkflowCanceled
import io.infinitic.avro.workflowManager.messages.AvroWorkflowCompleted
import io.infinitic.avro.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import io.infinitic.avro.workflowManager.messages.envelopes.AvroForWorkflowEngineMessageType
import io.infinitic.avro.workflowManager.states.AvroWorkflowState
import org.apache.avro.specific.SpecificRecordBase

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

    /**
     *  State <-> Avro State
     */
    fun fromStorage(avro: AvroWorkflowState) = WorkflowState(
        workflowId = WorkflowId(avro.workflowId),
        parentWorkflowId = avro.parentWorkflowId?.let { WorkflowId(it) },
        workflowName = WorkflowName(avro.workflowName),
        workflowOptions = convertJson(avro.workflowOptions),
        workflowMeta = convertJson(avro.workflowMeta),
        runningWorkflowTaskId = avro.currentWorkflowTaskId?.let { WorkflowTaskId(it) },
        workflowTaskIndex = WorkflowTaskIndex(avro.currentMessageIndex),
        methodRuns = avro.currentMethodRuns.map { convertJson<MethodRun>(it) }.toMutableList(),
        currentPropertiesNameHash = convertJson(avro.currentPropertiesNameHash),
        propertiesHashValue = convertJson(avro.propertiesHashValue),
        bufferedMessages = avro.bufferedMessages.map { fromWorkflowEngine(it) }.toMutableList()
    )

    fun toStorage(state: WorkflowState) = AvroWorkflowState
        .newBuilder()
        .setWorkflowId("${state.workflowId}")
        .setParentWorkflowId(state.parentWorkflowId?.toString())
        .setWorkflowName("${state.workflowName}")
        .setWorkflowOptions(convertJson(state.workflowOptions))
        .setWorkflowMeta(convertJson(state.workflowMeta))
        .setCurrentWorkflowTaskId(state.runningWorkflowTaskId?.toString())
        .setCurrentMessageIndex(convertJson(state.workflowTaskIndex))
        .setCurrentMethodRuns(state.methodRuns.map { convertJson<AvroMethodRun>(it) })
        .setCurrentPropertiesNameHash(convertJson(state.currentPropertiesNameHash))
        .setPropertiesHashValue(convertJson(state.propertiesHashValue))
        .setBufferedMessages(state.bufferedMessages.map { toWorkflowEngine(it) })
        .build()

    /**
     *  Avro message <-> Avro Envelope
     */
    fun addEnvelopeToWorkflowEngineMessage(message: SpecificRecordBase): AvroEnvelopeForWorkflowEngine {
        val builder = AvroEnvelopeForWorkflowEngine.newBuilder()
        when (message) {
            is AvroCancelWorkflow -> builder.apply {
                workflowId = message.workflowId
                avroCancelWorkflow = message
                type = AvroForWorkflowEngineMessageType.AvroCancelWorkflow
            }
            is AvroChildWorkflowCanceled -> builder.apply {
                workflowId = message.workflowId
                avroChildWorkflowCanceled = message
                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled
            }
            is AvroChildWorkflowCompleted -> builder.apply {
                workflowId = message.workflowId
                avroChildWorkflowCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted
            }
            is AvroWorkflowTaskCompleted -> builder.apply {
                workflowId = message.workflowId
                avroWorkflowTaskCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroWorkflowTaskCompleted
            }
            is AvroWorkflowTaskDispatched -> builder.apply {
                workflowId = message.workflowId
                avroWorkflowTaskDispatched = message
                type = AvroForWorkflowEngineMessageType.AvroWorkflowTaskDispatched
            }
            is AvroDelayCompleted -> builder.apply {
                workflowId = message.workflowId
                avroDelayCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroDelayCompleted
            }
            is AvroDispatchWorkflow -> builder.apply {
                workflowId = message.workflowId
                avroDispatchWorkflow = message
                type = AvroForWorkflowEngineMessageType.AvroDispatchWorkflow
            }
            is AvroEventReceived -> builder.apply {
                workflowId = message.workflowId
                avroEventReceived = message
                type = AvroForWorkflowEngineMessageType.AvroEventReceived
            }
            is AvroTaskCanceled -> builder.apply {
                workflowId = message.workflowId
                avroTaskCanceled = message
                type = AvroForWorkflowEngineMessageType.AvroTaskCanceled
            }
            is AvroTaskCompleted -> builder.apply {
                workflowId = message.workflowId
                avroTaskCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroTaskCompleted
            }
            is AvroTaskDispatched -> builder.apply {
                workflowId = message.workflowId
                avroTaskDispatched = message
                type = AvroForWorkflowEngineMessageType.AvroTaskDispatched
            }
            is AvroWorkflowCanceled -> builder.apply {
                workflowId = message.workflowId
                avroWorkflowCanceled = message
                type = AvroForWorkflowEngineMessageType.AvroWorkflowCanceled
            }
            is AvroWorkflowCompleted -> builder.apply {
                workflowId = message.workflowId
                avroWorkflowCompleted = message
                type = AvroForWorkflowEngineMessageType.AvroWorkflowCompleted
            }
            else -> throw RuntimeException("Unknown AvroWorkflowEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun removeEnvelopeFromWorkflowEngineMessage(input: AvroEnvelopeForWorkflowEngine): SpecificRecordBase = when (input.type) {
        AvroForWorkflowEngineMessageType.AvroCancelWorkflow -> input.avroCancelWorkflow
        AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled -> input.avroChildWorkflowCanceled
        AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted -> input.avroChildWorkflowCompleted
        AvroForWorkflowEngineMessageType.AvroWorkflowTaskCompleted -> input.avroWorkflowTaskCompleted
        AvroForWorkflowEngineMessageType.AvroWorkflowTaskDispatched -> input.avroWorkflowTaskDispatched
        AvroForWorkflowEngineMessageType.AvroDelayCompleted -> input.avroDelayCompleted
        AvroForWorkflowEngineMessageType.AvroDispatchWorkflow -> input.avroDispatchWorkflow
        AvroForWorkflowEngineMessageType.AvroEventReceived -> input.avroEventReceived
        AvroForWorkflowEngineMessageType.AvroTaskCanceled -> input.avroTaskCanceled
        AvroForWorkflowEngineMessageType.AvroTaskCompleted -> input.avroTaskCompleted
        AvroForWorkflowEngineMessageType.AvroTaskDispatched -> input.avroTaskDispatched
        AvroForWorkflowEngineMessageType.AvroWorkflowCanceled -> input.avroWorkflowCanceled
        AvroForWorkflowEngineMessageType.AvroWorkflowCompleted -> input.avroWorkflowCompleted
        null -> throw Exception("Null type in $input")
    }

    /**
     *  Message <-> Avro Envelope
     */

    fun toWorkflowEngine(message: WorkflowEngineMessage): AvroEnvelopeForWorkflowEngine =
        addEnvelopeToWorkflowEngineMessage(toAvroMessage(message))

    fun fromWorkflowEngine(avro: AvroEnvelopeForWorkflowEngine) =
        fromAvroMessage(removeEnvelopeFromWorkflowEngineMessage(avro)) as WorkflowEngineMessage

    /**
     *  Message <-> Avro Message
     */

    fun fromAvroMessage(avro: SpecificRecordBase): WorkflowEngineMessage = when (avro) {
        is AvroCancelWorkflow -> fromAvroMessage(avro)
        is AvroChildWorkflowCanceled -> fromAvroMessage(avro)
        is AvroChildWorkflowCompleted -> fromAvroMessage(avro)
        is AvroWorkflowTaskCompleted -> fromAvroMessage(avro)
        is AvroWorkflowTaskDispatched -> fromAvroMessage(avro)
        is AvroDelayCompleted -> fromAvroMessage(avro)
        is AvroDispatchWorkflow -> fromAvroMessage(avro)
        is AvroEventReceived -> fromAvroMessage(avro)
        is AvroTaskCanceled -> fromAvroMessage(avro)
        is AvroTaskCompleted -> fromAvroMessage(avro)
        is AvroTaskDispatched -> fromAvroMessage(avro)
        is AvroWorkflowCanceled -> fromAvroMessage(avro)
        is AvroWorkflowCompleted -> fromAvroMessage(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    private fun fromAvroMessage(avro: AvroCancelWorkflow) = convertJson<CancelWorkflow>(avro)
    private fun fromAvroMessage(avro: AvroChildWorkflowCanceled) = convertJson<ChildWorkflowCanceled>(avro)
    private fun fromAvroMessage(avro: AvroChildWorkflowCompleted) = convertJson<ChildWorkflowCompleted>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowTaskCompleted) = convertJson<WorkflowTaskCompleted>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowTaskDispatched) = convertJson<WorkflowTaskDispatched>(avro)
    private fun fromAvroMessage(avro: AvroDelayCompleted) = convertJson<TimerCompleted>(avro)
    private fun fromAvroMessage(avro: AvroDispatchWorkflow) = convertJson<DispatchWorkflow>(avro)
    private fun fromAvroMessage(avro: AvroEventReceived) = convertJson<ObjectReceived>(avro)
    private fun fromAvroMessage(avro: AvroTaskCanceled) = convertJson<TaskCanceled>(avro)
    private fun fromAvroMessage(avro: AvroTaskCompleted) = convertJson<TaskCompleted>(avro)
    private fun fromAvroMessage(avro: AvroTaskDispatched) = convertJson<TaskDispatched>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowCanceled) = convertJson<WorkflowCanceled>(avro)
    private fun fromAvroMessage(avro: AvroWorkflowCompleted) = convertJson<WorkflowCompleted>(avro)

    fun toAvroMessage(message: WorkflowEngineMessage): SpecificRecordBase = when (message) {
        is CancelWorkflow -> AvroConverter.toAvroMessage(message)
        is ChildWorkflowCanceled -> AvroConverter.toAvroMessage(message)
        is ChildWorkflowCompleted -> AvroConverter.toAvroMessage(message)
        is WorkflowTaskCompleted -> AvroConverter.toAvroMessage(message)
        is WorkflowTaskDispatched -> AvroConverter.toAvroMessage(message)
        is TimerCompleted -> AvroConverter.toAvroMessage(message)
        is DispatchWorkflow -> AvroConverter.toAvroMessage(message)
        is ObjectReceived -> AvroConverter.toAvroMessage(message)
        is TaskCanceled -> AvroConverter.toAvroMessage(message)
        is TaskCompleted -> AvroConverter.toAvroMessage(message)
        is TaskDispatched -> AvroConverter.toAvroMessage(message)
        is WorkflowCanceled -> AvroConverter.toAvroMessage(message)
        is WorkflowCompleted -> AvroConverter.toAvroMessage(message)
    }

    private fun toAvroMessage(message: CancelWorkflow) = convertJson<AvroCancelWorkflow>(message)
    private fun toAvroMessage(message: ChildWorkflowCanceled) = convertJson<AvroChildWorkflowCanceled>(message)
    private fun toAvroMessage(message: ChildWorkflowCompleted) = convertJson<AvroChildWorkflowCompleted>(message)
    private fun toAvroMessage(message: WorkflowTaskCompleted) = convertJson<AvroWorkflowTaskCompleted>(message)
    private fun toAvroMessage(message: WorkflowTaskDispatched) = convertJson<AvroWorkflowTaskDispatched>(message)
    private fun toAvroMessage(message: TimerCompleted) = convertJson<AvroDelayCompleted>(message)
    private fun toAvroMessage(message: DispatchWorkflow) = convertJson<AvroDispatchWorkflow>(message)
    private fun toAvroMessage(message: ObjectReceived) = convertJson<AvroEventReceived>(message)
    private fun toAvroMessage(message: TaskCanceled) = convertJson<AvroTaskCanceled>(message)
    private fun toAvroMessage(message: TaskCompleted) = convertJson<AvroTaskCompleted>(message)
    private fun toAvroMessage(message: TaskDispatched) = convertJson<AvroTaskDispatched>(message)
    private fun toAvroMessage(message: WorkflowCanceled) = convertJson<AvroWorkflowCanceled>(message)
    private fun toAvroMessage(message: WorkflowCompleted) = convertJson<AvroWorkflowCompleted>(message)

    /**
     *  Mapping function by Json serialization/deserialization
     */
    inline fun <reified T : Any> convertJson(from: Any?): T = Json.parse(Json.stringify(from))
}
