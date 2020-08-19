package io.infinitic.taskManager.common.avro

import io.infinitic.common.data.SerializedData
import io.infinitic.common.json.Json
import io.infinitic.taskManager.common.messages.CancelTask
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage
import io.infinitic.taskManager.common.messages.TaskAttemptCompleted
import io.infinitic.taskManager.common.messages.TaskAttemptDispatched
import io.infinitic.taskManager.common.messages.TaskAttemptFailed
import io.infinitic.taskManager.common.messages.TaskAttemptStarted
import io.infinitic.taskManager.common.messages.TaskCanceled
import io.infinitic.taskManager.common.messages.TaskCompleted
import io.infinitic.taskManager.common.messages.TaskCreated
import io.infinitic.taskManager.common.messages.TaskStatusUpdated
import io.infinitic.taskManager.common.messages.Message
import io.infinitic.taskManager.common.messages.RetryTask
import io.infinitic.taskManager.common.messages.RetryTaskAttempt
import io.infinitic.taskManager.common.messages.RunTask
import io.infinitic.taskManager.common.states.TaskEngineState
import io.infinitic.taskManager.common.states.MonitoringGlobalState
import io.infinitic.taskManager.common.states.MonitoringPerNameState
import io.infinitic.taskManager.common.states.State
import io.infinitic.taskManager.data.AvroSerializedData
import io.infinitic.taskManager.messages.AvroCancelTask
import io.infinitic.taskManager.messages.AvroDispatchTask
import io.infinitic.taskManager.messages.AvroTaskAttemptCompleted
import io.infinitic.taskManager.messages.AvroTaskAttemptDispatched
import io.infinitic.taskManager.messages.AvroTaskAttemptFailed
import io.infinitic.taskManager.messages.AvroTaskAttemptStarted
import io.infinitic.taskManager.messages.AvroTaskCanceled
import io.infinitic.taskManager.messages.AvroTaskCompleted
import io.infinitic.taskManager.messages.AvroTaskCreated
import io.infinitic.taskManager.messages.AvroTaskStatusUpdated
import io.infinitic.taskManager.messages.AvroRetryTask
import io.infinitic.taskManager.messages.AvroRetryTaskAttempt
import io.infinitic.taskManager.messages.AvroRunTask
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.taskManager.messages.envelopes.AvroForTaskEngineMessageType
import io.infinitic.taskManager.messages.envelopes.AvroForMonitoringGlobalMessageType
import io.infinitic.taskManager.messages.envelopes.AvroForMonitoringPerNameMessageType
import io.infinitic.taskManager.messages.envelopes.AvroForWorkerMessageType
import io.infinitic.taskManager.states.AvroTaskEngineState
import io.infinitic.taskManager.states.AvroMonitoringGlobalState
import io.infinitic.taskManager.states.AvroMonitoringPerNameState
import org.apache.avro.specific.SpecificRecordBase
import java.nio.ByteBuffer

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

    /**
     *  State <-> Avro State
     */
    fun fromStorage(avro: SpecificRecordBase) = when (avro) {
        is AvroTaskEngineState -> fromStorage(avro)
        is AvroMonitoringGlobalState -> fromStorage(avro)
        is AvroMonitoringPerNameState -> fromStorage(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    fun fromStorage(avro: AvroTaskEngineState) = convertJson<TaskEngineState>(avro)
    fun fromStorage(avro: AvroMonitoringGlobalState) = convertJson<MonitoringGlobalState>(avro)
    fun fromStorage(avro: AvroMonitoringPerNameState) = convertJson<MonitoringPerNameState>(avro)

    fun toStorage(state: State) = when (state) {
        is TaskEngineState -> toStorage(state)
        is MonitoringGlobalState -> toStorage(state)
        is MonitoringPerNameState -> toStorage(state)
    }

    fun toStorage(state: TaskEngineState) = convertJson<AvroTaskEngineState>(state)
    fun toStorage(state: MonitoringGlobalState) = convertJson<AvroMonitoringGlobalState>(state)
    fun toStorage(state: MonitoringPerNameState) = convertJson<AvroMonitoringPerNameState>(state)

    /**
     *  Avro message <-> Avro Envelope
     */

    fun addEnvelopeToTaskEngineMessage(message: SpecificRecordBase): AvroEnvelopeForTaskEngine {
        val builder = AvroEnvelopeForTaskEngine.newBuilder()
        when (message) {
            is AvroCancelTask -> builder.apply {
                taskId = message.taskId
                cancelTask = message
                type = AvroForTaskEngineMessageType.CancelTask
            }
            is AvroDispatchTask -> builder.apply {
                taskId = message.taskId
                dispatchTask = message
                type = AvroForTaskEngineMessageType.DispatchTask
            }
            is AvroRetryTask -> builder.apply {
                taskId = message.taskId
                retryTask = message
                type = AvroForTaskEngineMessageType.RetryTask
            }
            is AvroRetryTaskAttempt -> builder.apply {
                taskId = message.taskId
                retryTaskAttempt = message
                type = AvroForTaskEngineMessageType.RetryTaskAttempt
            }
            is AvroTaskAttemptDispatched -> builder.apply {
                taskId = message.taskId
                taskAttemptDispatched = message
                type = AvroForTaskEngineMessageType.TaskAttemptDispatched
            }
            is AvroTaskAttemptCompleted -> builder.apply {
                taskId = message.taskId
                taskAttemptCompleted = message
                type = AvroForTaskEngineMessageType.TaskAttemptCompleted
            }
            is AvroTaskAttemptFailed -> builder.apply {
                taskId = message.taskId
                taskAttemptFailed = message
                type = AvroForTaskEngineMessageType.TaskAttemptFailed
            }
            is AvroTaskAttemptStarted -> builder.apply {
                taskId = message.taskId
                taskAttemptStarted = message
                type = AvroForTaskEngineMessageType.TaskAttemptStarted
            }
            is AvroTaskCanceled -> builder.apply {
                taskId = message.taskId
                taskCanceled = message
                type = AvroForTaskEngineMessageType.TaskCanceled
            }
            is AvroTaskCompleted -> builder.apply {
                taskId = message.taskId
                taskCompleted = message
                type = AvroForTaskEngineMessageType.TaskCompleted
            }
            else -> throw Exception("Unknown AvroTaskEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun removeEnvelopeFromTaskEngineMessage(input: AvroEnvelopeForTaskEngine): SpecificRecordBase = when (input.type) {
        AvroForTaskEngineMessageType.CancelTask -> input.cancelTask
        AvroForTaskEngineMessageType.DispatchTask -> input.dispatchTask
        AvroForTaskEngineMessageType.RetryTask -> input.retryTask
        AvroForTaskEngineMessageType.RetryTaskAttempt -> input.retryTaskAttempt
        AvroForTaskEngineMessageType.TaskAttemptDispatched -> input.taskAttemptDispatched
        AvroForTaskEngineMessageType.TaskAttemptCompleted -> input.taskAttemptCompleted
        AvroForTaskEngineMessageType.TaskAttemptFailed -> input.taskAttemptFailed
        AvroForTaskEngineMessageType.TaskAttemptStarted -> input.taskAttemptStarted
        AvroForTaskEngineMessageType.TaskCanceled -> input.taskCanceled
        AvroForTaskEngineMessageType.TaskCompleted -> input.taskCompleted
        null -> throw Exception("Null type in $input")
    }

    private fun addEnvelopeToMonitoringPerNameMessage(message: SpecificRecordBase): AvroEnvelopeForMonitoringPerName {
        val builder = AvroEnvelopeForMonitoringPerName.newBuilder()
        when (message) {
            is AvroTaskStatusUpdated -> builder.apply {
                taskName = message.taskName
                taskStatusUpdated = message
                type = AvroForMonitoringPerNameMessageType.TaskStatusUpdated
            }
            else -> throw Exception("Unknown AvroMonitoringPerNameMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    private fun removeEnvelopeFromMonitoringPerNameMessage(input: AvroEnvelopeForMonitoringPerName): SpecificRecordBase = when (input.type) {
        AvroForMonitoringPerNameMessageType.TaskStatusUpdated -> input.taskStatusUpdated
        else -> throw Exception("Unknown AvroEnvelopeForMonitoringPerName: ${input::class.qualifiedName}")
    }

    private fun addEnvelopeToMonitoringGlobalMessage(message: SpecificRecordBase): AvroEnvelopeForMonitoringGlobal {
        val builder = AvroEnvelopeForMonitoringGlobal.newBuilder()
        when (message) {
            is AvroTaskCreated -> builder.apply {
                taskCreated = message
                type = AvroForMonitoringGlobalMessageType.TaskCreated
            }
            else -> throw Exception("Unknown AvroMonitoringglobalMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    private fun removeEnvelopeFromMonitoringGlobalMessage(input: AvroEnvelopeForMonitoringGlobal): SpecificRecordBase = when (input.type) {
        AvroForMonitoringGlobalMessageType.TaskCreated -> input.taskCreated
        else -> throw Exception("Unknown AvroEnvelopeForMonitoringGlobal: ${input::class.qualifiedName}")
    }

    fun addEnvelopeToWorkerMessage(message: SpecificRecordBase): AvroEnvelopeForWorker {
        val builder = AvroEnvelopeForWorker.newBuilder()
        when (message) {
            is AvroRunTask -> builder.apply {
                taskName = message.taskName
                runTask = message
                type = AvroForWorkerMessageType.RunTask
            }
            else -> throw Exception("Unknown AvroForWorkerMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun removeEnvelopeFromWorkerMessage(input: AvroEnvelopeForWorker): SpecificRecordBase = when (input.type!!) {
        AvroForWorkerMessageType.RunTask -> input.runTask
    }

    /**
     *  Message <-> Avro Envelope
     */

    fun toTaskEngine(message: ForTaskEngineMessage): AvroEnvelopeForTaskEngine =
        addEnvelopeToTaskEngineMessage(toAvroMessage(message))

    fun fromTaskEngine(input: AvroEnvelopeForTaskEngine) =
        fromAvroMessage(removeEnvelopeFromTaskEngineMessage(input)) as ForTaskEngineMessage

    fun toMonitoringPerName(message: ForMonitoringPerNameMessage): AvroEnvelopeForMonitoringPerName =
        addEnvelopeToMonitoringPerNameMessage(toAvroMessage(message))

    fun fromMonitoringPerName(input: AvroEnvelopeForMonitoringPerName) =
        fromAvroMessage(removeEnvelopeFromMonitoringPerNameMessage(input)) as ForMonitoringPerNameMessage

    fun toMonitoringGlobal(message: ForMonitoringGlobalMessage): AvroEnvelopeForMonitoringGlobal =
        addEnvelopeToMonitoringGlobalMessage(toAvroMessage(message))

    fun fromMonitoringGlobal(input: AvroEnvelopeForMonitoringGlobal) =
        fromAvroMessage(removeEnvelopeFromMonitoringGlobalMessage(input)) as ForMonitoringGlobalMessage

    fun toWorkers(message: ForWorkerMessage): AvroEnvelopeForWorker =
        addEnvelopeToWorkerMessage(toAvroMessage(message))

    fun fromWorkers(input: AvroEnvelopeForWorker) =
        fromAvroMessage(removeEnvelopeFromWorkerMessage(input)) as ForWorkerMessage

    /**
     *  Message <-> Avro Message
     */

    fun fromAvroMessage(avro: SpecificRecordBase) = when (avro) {
        is AvroCancelTask -> fromAvroMessage(avro)
        is AvroDispatchTask -> fromAvroMessage(avro)
        is AvroTaskAttemptCompleted -> fromAvroMessage(avro)
        is AvroTaskAttemptDispatched -> fromAvroMessage(avro)
        is AvroTaskAttemptFailed -> fromAvroMessage(avro)
        is AvroTaskAttemptStarted -> fromAvroMessage(avro)
        is AvroTaskCanceled -> fromAvroMessage(avro)
        is AvroTaskCompleted -> fromAvroMessage(avro)
        is AvroTaskCreated -> fromAvroMessage(avro)
        is AvroTaskStatusUpdated -> fromAvroMessage(avro)
        is AvroRetryTask -> fromAvroMessage(avro)
        is AvroRetryTaskAttempt -> fromAvroMessage(avro)
        is AvroRunTask -> fromAvroMessage(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    private fun fromAvroMessage(avro: AvroCancelTask) = convertJson<CancelTask>(avro)
    private fun fromAvroMessage(avro: AvroDispatchTask) = convertJson<DispatchTask>(avro)
    private fun fromAvroMessage(avro: AvroTaskAttemptCompleted) = convertJson<TaskAttemptCompleted>(avro)
    private fun fromAvroMessage(avro: AvroTaskAttemptDispatched) = convertJson<TaskAttemptDispatched>(avro)
    private fun fromAvroMessage(avro: AvroTaskAttemptFailed) = convertJson<TaskAttemptFailed>(avro)
    private fun fromAvroMessage(avro: AvroTaskAttemptStarted) = convertJson<TaskAttemptStarted>(avro)
    private fun fromAvroMessage(avro: AvroTaskCanceled) = convertJson<TaskCanceled>(avro)
    private fun fromAvroMessage(avro: AvroTaskCompleted) = convertJson<TaskCompleted>(avro)
    private fun fromAvroMessage(avro: AvroTaskCreated) = convertJson<TaskCreated>(avro)
    private fun fromAvroMessage(avro: AvroTaskStatusUpdated) = convertJson<TaskStatusUpdated>(avro)
    private fun fromAvroMessage(avro: AvroRetryTask) = convertJson<RetryTask>(avro)
    private fun fromAvroMessage(avro: AvroRetryTaskAttempt) = convertJson<RetryTaskAttempt>(avro)
    private fun fromAvroMessage(avro: AvroRunTask) = convertJson<RunTask>(avro)

    fun toAvroMessage(msg: Message) = when (msg) {
        is CancelTask -> toAvroMessage(msg)
        is DispatchTask -> toAvroMessage(msg)
        is TaskAttemptCompleted -> toAvroMessage(msg)
        is TaskAttemptDispatched -> toAvroMessage(msg)
        is TaskAttemptFailed -> toAvroMessage(msg)
        is TaskAttemptStarted -> toAvroMessage(msg)
        is TaskCanceled -> toAvroMessage(msg)
        is TaskCompleted -> toAvroMessage(msg)
        is TaskCreated -> toAvroMessage(msg)
        is TaskStatusUpdated -> toAvroMessage(msg)
        is RetryTask -> toAvroMessage(msg)
        is RetryTaskAttempt -> toAvroMessage(msg)
        is RunTask -> toAvroMessage(msg)
    }

    private fun toAvroMessage(message: CancelTask) = convertJson<AvroCancelTask>(message)
    private fun toAvroMessage(message: DispatchTask) = convertJson<AvroDispatchTask>(message)
    private fun toAvroMessage(message: TaskAttemptCompleted) = convertJson<AvroTaskAttemptCompleted>(message)
    private fun toAvroMessage(message: TaskAttemptDispatched) = convertJson<AvroTaskAttemptDispatched>(message)
    private fun toAvroMessage(message: TaskAttemptFailed) = convertJson<AvroTaskAttemptFailed>(message)
    private fun toAvroMessage(message: TaskAttemptStarted) = convertJson<AvroTaskAttemptStarted>(message)
    private fun toAvroMessage(message: TaskCanceled) = convertJson<AvroTaskCanceled>(message)
    private fun toAvroMessage(message: TaskCompleted) = convertJson<AvroTaskCompleted>(message)
    private fun toAvroMessage(message: TaskCreated) = convertJson<AvroTaskCreated>(message)
    private fun toAvroMessage(message: TaskStatusUpdated) = convertJson<AvroTaskStatusUpdated>(message)
    private fun toAvroMessage(message: RetryTask) = convertJson<AvroRetryTask>(message)
    private fun toAvroMessage(message: RetryTaskAttempt) = convertJson<AvroRetryTaskAttempt>(message)
    private fun toAvroMessage(message: RunTask) = convertJson<AvroRunTask>(message)

    /**
     *  SerializedData to AvroSerializedData
     */

    fun fromAvroSerializedData(avro: AvroSerializedData) = SerializedData(
        avro.bytes.array(),
        avro.type,
        avro.meta.mapValues { it.value.array() }
    )

    fun toAvroSerializedData(data: SerializedData): AvroSerializedData = AvroSerializedData
        .newBuilder()
        .setType(data.type)
        .setBytes(ByteBuffer.wrap(data.bytes))
        .setMeta(data.meta.mapValues { ByteBuffer.wrap(it.value) })
        .build()

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convertJson(from: Any): T = Json.parse(Json.stringify(from))
}
