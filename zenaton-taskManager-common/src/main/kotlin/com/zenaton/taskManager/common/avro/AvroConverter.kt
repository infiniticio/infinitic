package com.zenaton.taskManager.common.avro

import com.zenaton.common.data.SerializedData
import com.zenaton.common.json.Json
import com.zenaton.taskManager.common.messages.CancelJob
import com.zenaton.taskManager.common.messages.DispatchJob
import com.zenaton.taskManager.common.messages.ForJobEngineMessage
import com.zenaton.taskManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.taskManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.taskManager.common.messages.ForWorkerMessage
import com.zenaton.taskManager.common.messages.JobAttemptCompleted
import com.zenaton.taskManager.common.messages.JobAttemptDispatched
import com.zenaton.taskManager.common.messages.JobAttemptFailed
import com.zenaton.taskManager.common.messages.JobAttemptStarted
import com.zenaton.taskManager.common.messages.JobCanceled
import com.zenaton.taskManager.common.messages.JobCompleted
import com.zenaton.taskManager.common.messages.JobCreated
import com.zenaton.taskManager.common.messages.JobStatusUpdated
import com.zenaton.taskManager.common.messages.Message
import com.zenaton.taskManager.common.messages.RetryJob
import com.zenaton.taskManager.common.messages.RetryJobAttempt
import com.zenaton.taskManager.common.messages.RunJob
import com.zenaton.taskManager.common.states.JobEngineState
import com.zenaton.taskManager.common.states.MonitoringGlobalState
import com.zenaton.taskManager.common.states.MonitoringPerNameState
import com.zenaton.taskManager.common.states.State
import com.zenaton.taskManager.data.AvroSerializedData
import com.zenaton.taskManager.messages.AvroCancelJob
import com.zenaton.taskManager.messages.AvroDispatchJob
import com.zenaton.taskManager.messages.AvroJobAttemptCompleted
import com.zenaton.taskManager.messages.AvroJobAttemptDispatched
import com.zenaton.taskManager.messages.AvroJobAttemptFailed
import com.zenaton.taskManager.messages.AvroJobAttemptStarted
import com.zenaton.taskManager.messages.AvroJobCanceled
import com.zenaton.taskManager.messages.AvroJobCompleted
import com.zenaton.taskManager.messages.AvroJobCreated
import com.zenaton.taskManager.messages.AvroJobStatusUpdated
import com.zenaton.taskManager.messages.AvroRetryJob
import com.zenaton.taskManager.messages.AvroRetryJobAttempt
import com.zenaton.taskManager.messages.AvroRunJob
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.taskManager.messages.envelopes.AvroForJobEngineMessageType
import com.zenaton.taskManager.messages.envelopes.AvroForMonitoringGlobalMessageType
import com.zenaton.taskManager.messages.envelopes.AvroForMonitoringPerNameMessageType
import com.zenaton.taskManager.messages.envelopes.AvroForWorkerMessageType
import com.zenaton.taskManager.states.AvroJobEngineState
import com.zenaton.taskManager.states.AvroMonitoringGlobalState
import com.zenaton.taskManager.states.AvroMonitoringPerNameState
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
        is AvroJobEngineState -> fromStorage(avro)
        is AvroMonitoringGlobalState -> fromStorage(avro)
        is AvroMonitoringPerNameState -> fromStorage(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    fun fromStorage(avro: AvroJobEngineState) = convertJson<JobEngineState>(avro)
    fun fromStorage(avro: AvroMonitoringGlobalState) = convertJson<MonitoringGlobalState>(avro)
    fun fromStorage(avro: AvroMonitoringPerNameState) = convertJson<MonitoringPerNameState>(avro)

    fun toStorage(state: State) = when (state) {
        is JobEngineState -> toStorage(state)
        is MonitoringGlobalState -> toStorage(state)
        is MonitoringPerNameState -> toStorage(state)
    }

    fun toStorage(state: JobEngineState) = convertJson<AvroJobEngineState>(state)
    fun toStorage(state: MonitoringGlobalState) = convertJson<AvroMonitoringGlobalState>(state)
    fun toStorage(state: MonitoringPerNameState) = convertJson<AvroMonitoringPerNameState>(state)

    /**
     *  Avro message <-> Avro Envelope
     */

    fun addEnvelopeToJobEngineMessage(message: SpecificRecordBase): AvroEnvelopeForJobEngine {
        val builder = AvroEnvelopeForJobEngine.newBuilder()
        when (message) {
            is AvroCancelJob -> builder.apply {
                jobId = message.jobId
                cancelJob = message
                type = AvroForJobEngineMessageType.CancelJob
            }
            is AvroDispatchJob -> builder.apply {
                jobId = message.jobId
                dispatchJob = message
                type = AvroForJobEngineMessageType.DispatchJob
            }
            is AvroRetryJob -> builder.apply {
                jobId = message.jobId
                retryJob = message
                type = AvroForJobEngineMessageType.RetryJob
            }
            is AvroRetryJobAttempt -> builder.apply {
                jobId = message.jobId
                retryJobAttempt = message
                type = AvroForJobEngineMessageType.RetryJobAttempt
            }
            is AvroJobAttemptDispatched -> builder.apply {
                jobId = message.jobId
                jobAttemptDispatched = message
                type = AvroForJobEngineMessageType.JobAttemptDispatched
            }
            is AvroJobAttemptCompleted -> builder.apply {
                jobId = message.jobId
                jobAttemptCompleted = message
                type = AvroForJobEngineMessageType.JobAttemptCompleted
            }
            is AvroJobAttemptFailed -> builder.apply {
                jobId = message.jobId
                jobAttemptFailed = message
                type = AvroForJobEngineMessageType.JobAttemptFailed
            }
            is AvroJobAttemptStarted -> builder.apply {
                jobId = message.jobId
                jobAttemptStarted = message
                type = AvroForJobEngineMessageType.JobAttemptStarted
            }
            is AvroJobCanceled -> builder.apply {
                jobId = message.jobId
                jobCanceled = message
                type = AvroForJobEngineMessageType.JobCanceled
            }
            is AvroJobCompleted -> builder.apply {
                jobId = message.jobId
                jobCompleted = message
                type = AvroForJobEngineMessageType.JobCompleted
            }
            else -> throw Exception("Unknown AvroJobEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun removeEnvelopeFromJobEngineMessage(input: AvroEnvelopeForJobEngine): SpecificRecordBase = when (input.type) {
        AvroForJobEngineMessageType.CancelJob -> input.cancelJob
        AvroForJobEngineMessageType.DispatchJob -> input.dispatchJob
        AvroForJobEngineMessageType.RetryJob -> input.retryJob
        AvroForJobEngineMessageType.RetryJobAttempt -> input.retryJobAttempt
        AvroForJobEngineMessageType.JobAttemptDispatched -> input.jobAttemptDispatched
        AvroForJobEngineMessageType.JobAttemptCompleted -> input.jobAttemptCompleted
        AvroForJobEngineMessageType.JobAttemptFailed -> input.jobAttemptFailed
        AvroForJobEngineMessageType.JobAttemptStarted -> input.jobAttemptStarted
        AvroForJobEngineMessageType.JobCanceled -> input.jobCanceled
        AvroForJobEngineMessageType.JobCompleted -> input.jobCompleted
        null -> throw Exception("Null type in $input")
    }

    private fun addEnvelopeToMonitoringPerNameMessage(message: SpecificRecordBase): AvroEnvelopeForMonitoringPerName {
        val builder = AvroEnvelopeForMonitoringPerName.newBuilder()
        when (message) {
            is AvroJobStatusUpdated -> builder.apply {
                jobName = message.jobName
                jobStatusUpdated = message
                type = AvroForMonitoringPerNameMessageType.JobStatusUpdated
            }
            else -> throw Exception("Unknown AvroMonitoringPerNameMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    private fun removeEnvelopeFromMonitoringPerNameMessage(input: AvroEnvelopeForMonitoringPerName): SpecificRecordBase = when (input.type) {
        AvroForMonitoringPerNameMessageType.JobStatusUpdated -> input.jobStatusUpdated
        else -> throw Exception("Unknown AvroEnvelopeForMonitoringPerName: ${input::class.qualifiedName}")
    }

    private fun addEnvelopeToMonitoringGlobalMessage(message: SpecificRecordBase): AvroEnvelopeForMonitoringGlobal {
        val builder = AvroEnvelopeForMonitoringGlobal.newBuilder()
        when (message) {
            is AvroJobCreated -> builder.apply {
                jobCreated = message
                type = AvroForMonitoringGlobalMessageType.JobCreated
            }
            else -> throw Exception("Unknown AvroMonitoringglobalMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    private fun removeEnvelopeFromMonitoringGlobalMessage(input: AvroEnvelopeForMonitoringGlobal): SpecificRecordBase = when (input.type) {
        AvroForMonitoringGlobalMessageType.JobCreated -> input.jobCreated
        else -> throw Exception("Unknown AvroEnvelopeForMonitoringGlobal: ${input::class.qualifiedName}")
    }

    fun addEnvelopeToWorkerMessage(message: SpecificRecordBase): AvroEnvelopeForWorker {
        val builder = AvroEnvelopeForWorker.newBuilder()
        when (message) {
            is AvroRunJob -> builder.apply {
                jobName = message.jobName
                runJob = message
                type = AvroForWorkerMessageType.RunJob
            }
            else -> throw Exception("Unknown AvroForWorkerMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun removeEnvelopeFromWorkerMessage(input: AvroEnvelopeForWorker): SpecificRecordBase = when (input.type!!) {
        AvroForWorkerMessageType.RunJob -> input.runJob
    }

    /**
     *  Message <-> Avro Envelope
     */

    fun toJobEngine(message: ForJobEngineMessage): AvroEnvelopeForJobEngine =
        addEnvelopeToJobEngineMessage(toAvroMessage(message))

    fun fromJobEngine(input: AvroEnvelopeForJobEngine) =
        fromAvroMessage(removeEnvelopeFromJobEngineMessage(input)) as ForJobEngineMessage

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
        is AvroCancelJob -> fromAvroMessage(avro)
        is AvroDispatchJob -> fromAvroMessage(avro)
        is AvroJobAttemptCompleted -> fromAvroMessage(avro)
        is AvroJobAttemptDispatched -> fromAvroMessage(avro)
        is AvroJobAttemptFailed -> fromAvroMessage(avro)
        is AvroJobAttemptStarted -> fromAvroMessage(avro)
        is AvroJobCanceled -> fromAvroMessage(avro)
        is AvroJobCompleted -> fromAvroMessage(avro)
        is AvroJobCreated -> fromAvroMessage(avro)
        is AvroJobStatusUpdated -> fromAvroMessage(avro)
        is AvroRetryJob -> fromAvroMessage(avro)
        is AvroRetryJobAttempt -> fromAvroMessage(avro)
        is AvroRunJob -> fromAvroMessage(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    private fun fromAvroMessage(avro: AvroCancelJob) = convertJson<CancelJob>(avro)
    private fun fromAvroMessage(avro: AvroDispatchJob) = convertJson<DispatchJob>(avro)
    private fun fromAvroMessage(avro: AvroJobAttemptCompleted) = convertJson<JobAttemptCompleted>(avro)
    private fun fromAvroMessage(avro: AvroJobAttemptDispatched) = convertJson<JobAttemptDispatched>(avro)
    private fun fromAvroMessage(avro: AvroJobAttemptFailed) = convertJson<JobAttemptFailed>(avro)
    private fun fromAvroMessage(avro: AvroJobAttemptStarted) = convertJson<JobAttemptStarted>(avro)
    private fun fromAvroMessage(avro: AvroJobCanceled) = convertJson<JobCanceled>(avro)
    private fun fromAvroMessage(avro: AvroJobCompleted) = convertJson<JobCompleted>(avro)
    private fun fromAvroMessage(avro: AvroJobCreated) = convertJson<JobCreated>(avro)
    private fun fromAvroMessage(avro: AvroJobStatusUpdated) = convertJson<JobStatusUpdated>(avro)
    private fun fromAvroMessage(avro: AvroRetryJob) = convertJson<RetryJob>(avro)
    private fun fromAvroMessage(avro: AvroRetryJobAttempt) = convertJson<RetryJobAttempt>(avro)
    private fun fromAvroMessage(avro: AvroRunJob) = convertJson<RunJob>(avro)

    fun toAvroMessage(msg: Message) = when (msg) {
        is CancelJob -> toAvroMessage(msg)
        is DispatchJob -> toAvroMessage(msg)
        is JobAttemptCompleted -> toAvroMessage(msg)
        is JobAttemptDispatched -> toAvroMessage(msg)
        is JobAttemptFailed -> toAvroMessage(msg)
        is JobAttemptStarted -> toAvroMessage(msg)
        is JobCanceled -> toAvroMessage(msg)
        is JobCompleted -> toAvroMessage(msg)
        is JobCreated -> toAvroMessage(msg)
        is JobStatusUpdated -> toAvroMessage(msg)
        is RetryJob -> toAvroMessage(msg)
        is RetryJobAttempt -> toAvroMessage(msg)
        is RunJob -> toAvroMessage(msg)
    }

    private fun toAvroMessage(message: CancelJob) = convertJson<AvroCancelJob>(message)
    private fun toAvroMessage(message: DispatchJob) = convertJson<AvroDispatchJob>(message)
    private fun toAvroMessage(message: JobAttemptCompleted) = convertJson<AvroJobAttemptCompleted>(message)
    private fun toAvroMessage(message: JobAttemptDispatched) = convertJson<AvroJobAttemptDispatched>(message)
    private fun toAvroMessage(message: JobAttemptFailed) = convertJson<AvroJobAttemptFailed>(message)
    private fun toAvroMessage(message: JobAttemptStarted) = convertJson<AvroJobAttemptStarted>(message)
    private fun toAvroMessage(message: JobCanceled) = convertJson<AvroJobCanceled>(message)
    private fun toAvroMessage(message: JobCompleted) = convertJson<AvroJobCompleted>(message)
    private fun toAvroMessage(message: JobCreated) = convertJson<AvroJobCreated>(message)
    private fun toAvroMessage(message: JobStatusUpdated) = convertJson<AvroJobStatusUpdated>(message)
    private fun toAvroMessage(message: RetryJob) = convertJson<AvroRetryJob>(message)
    private fun toAvroMessage(message: RetryJobAttempt) = convertJson<AvroRetryJobAttempt>(message)
    private fun toAvroMessage(message: RunJob) = convertJson<AvroRunJob>(message)

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
