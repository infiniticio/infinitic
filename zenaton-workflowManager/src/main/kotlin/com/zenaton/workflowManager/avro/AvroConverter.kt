package com.zenaton.workflowManager.avro

import com.zenaton.commons.data.AvroSerializedData
import com.zenaton.commons.json.Json
import com.zenaton.commons.data.SerializedData
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessageType
import com.zenaton.workflowManager.engine.WorkflowEngineState
import com.zenaton.workflowManager.messages.AvroCancelWorkflow
import com.zenaton.workflowManager.messages.AvroChildWorkflowCanceled
import com.zenaton.workflowManager.messages.AvroChildWorkflowCompleted
import com.zenaton.workflowManager.messages.AvroDecisionCompleted
import com.zenaton.workflowManager.messages.AvroDecisionDispatched
import com.zenaton.workflowManager.messages.AvroDelayCompleted
import com.zenaton.workflowManager.messages.AvroDispatchWorkflow
import com.zenaton.workflowManager.messages.AvroEventReceived
import com.zenaton.workflowManager.messages.AvroTaskCanceled
import com.zenaton.workflowManager.messages.AvroTaskCompleted
import com.zenaton.workflowManager.messages.AvroTaskDispatched
import com.zenaton.workflowManager.messages.AvroWorkflowCanceled
import com.zenaton.workflowManager.messages.AvroWorkflowCompleted
import com.zenaton.workflowManager.messages.CancelWorkflow
import com.zenaton.workflowManager.messages.ChildWorkflowCanceled
import com.zenaton.workflowManager.messages.ChildWorkflowCompleted
import com.zenaton.workflowManager.messages.DecisionCompleted
import com.zenaton.workflowManager.messages.DecisionDispatched
import com.zenaton.workflowManager.messages.DelayCompleted
import com.zenaton.workflowManager.messages.DispatchDecision
import com.zenaton.workflowManager.messages.DispatchTask
import com.zenaton.workflowManager.messages.DispatchWorkflow
import com.zenaton.workflowManager.messages.EventReceived
import com.zenaton.workflowManager.messages.TaskCanceled
import com.zenaton.workflowManager.messages.TaskCompleted
import com.zenaton.workflowManager.messages.TaskDispatched
import com.zenaton.workflowManager.messages.WorkflowCanceled
import com.zenaton.workflowManager.messages.WorkflowCompleted
import com.zenaton.workflowManager.messages.envelopes.AvroForWorkflowEngineMessage
import com.zenaton.workflowManager.messages.envelopes.AvroForWorkflowEngineMessageType
import com.zenaton.workflowManager.messages.envelopes.ForDecidersMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkersMessage
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage
import com.zenaton.workflowManager.states.AvroWorkflowEngineState
import org.apache.avro.specific.SpecificRecordBase

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

    /**
     *  States
     */
    fun fromAvro(avro: AvroWorkflowEngineState) = convertJson<WorkflowEngineState>(avro)
    fun toAvro(state: WorkflowEngineState) = convertJson<AvroWorkflowEngineState>(state)

    /**
     *  Envelopes
     */

    fun toWorkflowEngine(message: ForWorkflowEngineMessage): AvroForWorkflowEngineMessage {
        val builder = AvroForWorkflowEngineMessage.newBuilder()
        builder.workflowId = message.workflowId.id
        when (message) {
            is CancelWorkflow -> builder.apply {
                avroCancelWorkflow = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroCancelWorkflow
            }
            is ChildWorkflowCanceled -> builder.apply {
                avroChildWorkflowCanceled = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled
            }
            is ChildWorkflowCompleted -> builder.apply {
                avroChildWorkflowCompleted = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted
            }
            is DecisionCompleted -> builder.apply {
                avroDecisionCompleted = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroDecisionCompleted
            }
            is DecisionDispatched -> builder.apply {
                avroDecisionDispatched = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroDecisionDispatched
            }
            is DelayCompleted -> builder.apply {
                avroDelayCompleted = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroDelayCompleted
            }
            is DispatchWorkflow -> builder.apply {
                avroDispatchWorkflow = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroDispatchWorkflow
            }
            is EventReceived -> builder.apply {
                avroEventReceived = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroEventReceived
            }
            is TaskCanceled -> builder.apply {
                avroTaskCanceled = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroTaskCanceled
            }
            is TaskCompleted -> builder.apply {
                avroTaskCompleted = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroTaskCompleted
            }
            is TaskDispatched -> builder.apply {
                avroTaskDispatched = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroTaskDispatched
            }
            is WorkflowCanceled -> builder.apply {
                avroWorkflowCanceled = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroWorkflowCanceled
            }
            is WorkflowCompleted -> builder.apply {
                avroWorkflowCompleted = convertToAvro(message)
                type = AvroForWorkflowEngineMessageType.AvroWorkflowCompleted
            }
            else -> throw Exception("Unknown ForWorkflowEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun fromWorkflowEngine(input: AvroForWorkflowEngineMessage): ForWorkflowEngineMessage {
        return when (input.type) {
            AvroForWorkflowEngineMessageType.AvroCancelWorkflow -> convertFromAvro(input.avroCancelWorkflow)
            AvroForWorkflowEngineMessageType.AvroChildWorkflowCanceled -> convertFromAvro(input.avroChildWorkflowCanceled)
            AvroForWorkflowEngineMessageType.AvroChildWorkflowCompleted -> convertFromAvro(input.avroChildWorkflowCompleted)
            AvroForWorkflowEngineMessageType.AvroDecisionCompleted -> convertFromAvro(input.avroDecisionCompleted)
            AvroForWorkflowEngineMessageType.AvroDecisionDispatched -> convertFromAvro(input.avroDecisionDispatched)
            AvroForWorkflowEngineMessageType.AvroDelayCompleted -> convertFromAvro(input.avroDelayCompleted)
            AvroForWorkflowEngineMessageType.AvroDispatchWorkflow -> convertFromAvro(input.avroDispatchWorkflow)
            AvroForWorkflowEngineMessageType.AvroEventReceived -> convertFromAvro(input.avroEventReceived)
            AvroForWorkflowEngineMessageType.AvroTaskCanceled -> convertFromAvro(input.avroTaskCanceled)
            AvroForWorkflowEngineMessageType.AvroTaskCompleted -> convertFromAvro(input.avroTaskCompleted)
            AvroForWorkflowEngineMessageType.AvroTaskDispatched -> convertFromAvro(input.avroTaskDispatched)
            AvroForWorkflowEngineMessageType.AvroWorkflowCanceled -> convertFromAvro(input.avroWorkflowCanceled)
            AvroForWorkflowEngineMessageType.AvroWorkflowCompleted -> convertFromAvro(input.avroWorkflowCompleted)
            else -> throw Exception("Unknown AvroForJobEngineMessage: ${input::class.qualifiedName}")
        }
    }

    fun toJobEngine(message: ForDecidersMessage): AvroForJobEngineMessage {
        val builder = AvroForJobEngineMessage.newBuilder()
        when (message) {
            is DispatchDecision -> builder.apply {
                dispatchJob = convertToAvro(message)
                type = AvroForJobEngineMessageType.DispatchJob
            }
            else -> throw Exception("Unknown ForWorkflowEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun toJobEngine(message: ForWorkersMessage): AvroForJobEngineMessage {
        val builder = AvroForJobEngineMessage.newBuilder()
        when (message) {
            is DispatchTask -> builder.apply {
                dispatchJob = convertToAvro(message)
                type = AvroForJobEngineMessageType.DispatchJob
            }
            else -> throw Exception("Unknown ForWorkflowEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    /**
     *  Messages
     */

    private fun convertFromAvro(avro: AvroCancelWorkflow) = convertJson<CancelWorkflow>(avro)
    private fun convertFromAvro(avro: AvroChildWorkflowCanceled) = convertJson<ChildWorkflowCanceled>(avro)
    private fun convertFromAvro(avro: AvroChildWorkflowCompleted) = convertJson<ChildWorkflowCompleted>(avro)
    private fun convertFromAvro(avro: AvroDecisionCompleted) = convertJson<DecisionCompleted>(avro)
    private fun convertFromAvro(avro: AvroDecisionDispatched) = convertJson<DecisionDispatched>(avro)
    private fun convertFromAvro(avro: AvroDelayCompleted) = convertJson<DelayCompleted>(avro)
    private fun convertFromAvro(avro: AvroDispatchWorkflow) = convertJson<DispatchWorkflow>(avro)
    private fun convertFromAvro(avro: AvroEventReceived) = convertJson<EventReceived>(avro)
    private fun convertFromAvro(avro: AvroTaskCanceled) = convertJson<TaskCanceled>(avro)
    private fun convertFromAvro(avro: AvroTaskCompleted) = convertJson<TaskCompleted>(avro)
    private fun convertFromAvro(avro: AvroTaskDispatched) = convertJson<TaskDispatched>(avro)
    private fun convertFromAvro(avro: AvroWorkflowCanceled) = convertJson<WorkflowCanceled>(avro)
    private fun convertFromAvro(avro: AvroWorkflowCompleted) = convertJson<WorkflowCompleted>(avro)

    private fun convertToAvro(message: CancelWorkflow) = convertJson<AvroCancelWorkflow>(message)
    private fun convertToAvro(message: ChildWorkflowCanceled) = convertJson<AvroChildWorkflowCanceled>(message)
    private fun convertToAvro(message: ChildWorkflowCompleted) = convertJson<AvroChildWorkflowCompleted>(message)
    private fun convertToAvro(message: DecisionCompleted) = convertJson<AvroDecisionCompleted>(message)
    private fun convertToAvro(message: DecisionDispatched) = convertJson<AvroDecisionDispatched>(message)
    private fun convertToAvro(message: DelayCompleted) = convertJson<AvroDelayCompleted>(message)
    private fun convertToAvro(message: DispatchWorkflow) = convertJson<AvroDispatchWorkflow>(message)
    private fun convertToAvro(message: EventReceived) = convertJson<AvroEventReceived>(message)
    private fun convertToAvro(message: TaskCanceled) = convertJson<AvroTaskCanceled>(message)
    private fun convertToAvro(message: TaskCompleted) = convertJson<AvroTaskCompleted>(message)
    private fun convertToAvro(message: TaskDispatched) = convertJson<AvroTaskDispatched>(message)
    private fun convertToAvro(message: WorkflowCanceled) = convertJson<AvroWorkflowCanceled>(message)
    private fun convertToAvro(message: WorkflowCompleted) = convertJson<AvroWorkflowCompleted>(message)

    private fun convertToAvro(message: DispatchTask) = AvroDispatchJob.newBuilder().apply {
        jobId = message.taskId.id
        workflowId = message.workflowId.id
        jobName = message.taskName.name
        jobInput = message.taskData.input.map { convertToAvro(it) }
    }.build()

    private fun convertToAvro(message: DispatchDecision) = AvroDispatchJob.newBuilder().apply {
        jobId = message.decisionId.id
        workflowId = message.workflowId.id
        jobName = message.workflowName.name
        jobInput = message.decisionData.input.map { convertToAvro(it) }
    }.build()

    /**
     *  Data
     */

    private fun convertToAvro(p: SerializedData) = convertJson<AvroSerializedData>(p)

    /**
     *  Any Message
     */

    fun convertFromAvro(avro: SpecificRecordBase): Any = when (avro) {
        is AvroCancelWorkflow -> convertFromAvro(avro)
        is AvroChildWorkflowCanceled -> convertFromAvro(avro)
        is AvroChildWorkflowCompleted -> convertFromAvro(avro)
        is AvroDecisionCompleted -> convertFromAvro(avro)
        is AvroDecisionDispatched -> convertFromAvro(avro)
        is AvroDelayCompleted -> convertFromAvro(avro)
        is AvroDispatchWorkflow -> convertFromAvro(avro)
        is AvroEventReceived -> convertFromAvro(avro)
        is AvroTaskCanceled -> convertFromAvro(avro)
        is AvroTaskCompleted -> convertFromAvro(avro)
        is AvroTaskDispatched -> convertFromAvro(avro)
        is AvroWorkflowCanceled -> convertFromAvro(avro)
        is AvroWorkflowCompleted -> convertFromAvro(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convertJson(from: Any): T = Json.parse(Json.stringify(from))
}
