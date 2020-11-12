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

package io.infinitic.common.serDe.avro

import io.infinitic.common.json.Json
import io.infinitic.common.tasks.messages.CancelTask
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.tasks.messages.TaskAttemptCompleted
import io.infinitic.common.tasks.messages.TaskAttemptDispatched
import io.infinitic.common.tasks.messages.TaskAttemptFailed
import io.infinitic.common.tasks.messages.TaskAttemptStarted
import io.infinitic.common.tasks.messages.TaskCanceled
import io.infinitic.common.tasks.messages.TaskCompleted
import io.infinitic.common.monitoringGlobal.messages.TaskCreated
import io.infinitic.common.monitoringPerName.messages.TaskStatusUpdated
import io.infinitic.common.tasks.messages.RetryTask
import io.infinitic.common.tasks.messages.RetryTaskAttempt
import io.infinitic.common.tasks.state.TaskState
import io.infinitic.common.monitoringGlobal.state.MonitoringGlobalState
import io.infinitic.common.monitoringPerName.state.MonitoringPerNameState
import io.infinitic.avro.taskManager.data.AvroSerializedData
import io.infinitic.avro.taskManager.data.AvroSerializedDataType
import io.infinitic.avro.taskManager.messages.AvroCancelTask
import io.infinitic.avro.taskManager.messages.AvroDispatchTask
import io.infinitic.avro.taskManager.messages.AvroTaskAttemptCompleted
import io.infinitic.avro.taskManager.messages.AvroTaskAttemptDispatched
import io.infinitic.avro.taskManager.messages.AvroTaskAttemptFailed
import io.infinitic.avro.taskManager.messages.AvroTaskAttemptStarted
import io.infinitic.avro.taskManager.messages.AvroTaskCanceled
import io.infinitic.avro.taskManager.messages.AvroTaskCompleted
import io.infinitic.avro.taskManager.messages.AvroTaskCreated
import io.infinitic.avro.taskManager.messages.AvroTaskStatusUpdated
import io.infinitic.avro.taskManager.messages.AvroRetryTask
import io.infinitic.avro.taskManager.messages.AvroRetryTaskAttempt
import io.infinitic.avro.taskManager.messages.AvroRunTask
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.avro.taskManager.messages.envelopes.AvroForTaskEngineMessageType
import io.infinitic.avro.taskManager.messages.envelopes.AvroForMonitoringGlobalMessageType
import io.infinitic.avro.taskManager.messages.envelopes.AvroForMonitoringPerNameMessageType
import io.infinitic.avro.taskManager.messages.envelopes.AvroForWorkerMessageType
import io.infinitic.avro.taskManager.data.states.AvroTaskEngineState
import io.infinitic.avro.taskManager.data.states.AvroMonitoringGlobalState
import io.infinitic.avro.taskManager.data.states.AvroMonitoringPerNameState
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.serDe.SerializedDataType
import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.tasks.messages.TaskEngineMessage
import io.infinitic.common.workers.messages.RunTask
import io.infinitic.common.workers.messages.WorkerMessage
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

    fun fromStorage(avro: AvroTaskEngineState) = convertJson<TaskState>(avro)
    fun fromStorage(avro: AvroMonitoringGlobalState) = convertJson<MonitoringGlobalState>(avro)
    fun fromStorage(avro: AvroMonitoringPerNameState) = convertJson<MonitoringPerNameState>(avro)

    fun toStorage(state: TaskState) = convertJson<AvroTaskEngineState>(state)
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

    fun toTaskEngine(message: TaskEngineMessage): AvroEnvelopeForTaskEngine =
        addEnvelopeToTaskEngineMessage(toAvroMessage(message))

    fun fromTaskEngine(input: AvroEnvelopeForTaskEngine) =
        fromAvroMessage(removeEnvelopeFromTaskEngineMessage(input)) as TaskEngineMessage

    fun toMonitoringPerName(message: MonitoringPerNameEngineMessage): AvroEnvelopeForMonitoringPerName =
        addEnvelopeToMonitoringPerNameMessage(toAvroMessage(message))

    fun fromMonitoringPerName(input: AvroEnvelopeForMonitoringPerName) =
        fromAvroMessage(removeEnvelopeFromMonitoringPerNameMessage(input)) as MonitoringPerNameEngineMessage

    fun toMonitoringGlobal(message: MonitoringGlobalMessage): AvroEnvelopeForMonitoringGlobal =
        addEnvelopeToMonitoringGlobalMessage(toAvroMessage(message))

    fun fromMonitoringGlobal(input: AvroEnvelopeForMonitoringGlobal) =
        fromAvroMessage(removeEnvelopeFromMonitoringGlobalMessage(input)) as MonitoringGlobalMessage

    fun toWorkers(message: WorkerMessage): AvroEnvelopeForWorker =
        addEnvelopeToWorkerMessage(toAvroMessage(message))

    fun fromWorkers(input: AvroEnvelopeForWorker) =
        fromAvroMessage(removeEnvelopeFromWorkerMessage(input)) as WorkerMessage

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

    fun toAvroMessage(msg: TaskEngineMessage) = when (msg) {
        is CancelTask -> toAvroMessage(msg)
        is DispatchTask -> toAvroMessage(msg)
        is TaskAttemptCompleted -> toAvroMessage(msg)
        is TaskAttemptDispatched -> toAvroMessage(msg)
        is TaskAttemptFailed -> toAvroMessage(msg)
        is TaskAttemptStarted -> toAvroMessage(msg)
        is TaskCanceled -> toAvroMessage(msg)
        is TaskCompleted -> toAvroMessage(msg)
        is RetryTask -> toAvroMessage(msg)
        is RetryTaskAttempt -> toAvroMessage(msg)
    }

    fun toAvroMessage(msg: MonitoringPerNameEngineMessage) = when (msg) {
        is TaskStatusUpdated -> toAvroMessage(msg)
    }

    fun toAvroMessage(msg: MonitoringGlobalMessage) = when (msg) {
        is TaskCreated -> toAvroMessage(msg)
    }
    fun toAvroMessage(msg: WorkerMessage) = when (msg) {
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
        fromAvroSerializedDataType(avro.type),
        avro.meta.mapValues { it.value.array() }
    )

    fun toAvroSerializedData(data: SerializedData): AvroSerializedData = AvroSerializedData
        .newBuilder()
        .setType(toAvroSerializedDataType(data.type))
        .setBytes(ByteBuffer.wrap(data.bytes))
        .setMeta(data.meta.mapValues { ByteBuffer.wrap(it.value) })
        .build()

    fun fromAvroSerializedDataType(avro: AvroSerializedDataType) = when (avro) {
        AvroSerializedDataType.AVRO -> SerializedDataType.AVRO_JAVA
        AvroSerializedDataType.BYTES -> SerializedDataType.BYTES
        AvroSerializedDataType.CUSTOM -> SerializedDataType.CUSTOM
        AvroSerializedDataType.JSON -> SerializedDataType.JSON_JACKSON
        AvroSerializedDataType.KOTLIN -> SerializedDataType.JSON_KOTLIN
        AvroSerializedDataType.NULL -> SerializedDataType.NULL
    }

    fun toAvroSerializedDataType(type: SerializedDataType) = when (type) {
        SerializedDataType.AVRO_JAVA -> AvroSerializedDataType.AVRO
        SerializedDataType.BYTES -> AvroSerializedDataType.BYTES
        SerializedDataType.CUSTOM -> AvroSerializedDataType.CUSTOM
        SerializedDataType.JSON_JACKSON -> AvroSerializedDataType.JSON
        SerializedDataType.JSON_KOTLIN -> AvroSerializedDataType.KOTLIN
        SerializedDataType.NULL -> AvroSerializedDataType.NULL
    }
    /**
     *  Mapping function by Json serialization/deserialization
     */
    inline fun <reified T : Any> convertJson(from: Any): T = Json.parse(Json.stringify(from))
}
