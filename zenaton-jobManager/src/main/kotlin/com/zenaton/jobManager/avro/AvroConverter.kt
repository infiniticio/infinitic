package com.zenaton.jobManager.avro

import com.zenaton.common.json.Json
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptDispatched
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroJobCanceled
import com.zenaton.jobManager.messages.AvroJobCompleted
import com.zenaton.jobManager.messages.AvroJobCreated
import com.zenaton.jobManager.messages.AvroJobStatusUpdated
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.messages.AvroRetryJobAttempt
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.CancelJob
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.jobManager.messages.ForJobEngineMessage
import com.zenaton.jobManager.messages.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.ForWorkerMessage
import com.zenaton.jobManager.messages.JobAttemptCompleted
import com.zenaton.jobManager.messages.JobAttemptDispatched
import com.zenaton.jobManager.messages.JobAttemptFailed
import com.zenaton.jobManager.messages.JobAttemptStarted
import com.zenaton.jobManager.messages.JobCanceled
import com.zenaton.jobManager.messages.JobCompleted
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.RetryJob
import com.zenaton.jobManager.messages.RetryJobAttempt
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessageType
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessageType
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessageType
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessageType
import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.jobManager.states.JobEngineState
import com.zenaton.jobManager.states.MonitoringGlobalState
import com.zenaton.jobManager.states.MonitoringPerNameState
import com.zenaton.jobManager.states.State
import org.apache.avro.specific.SpecificRecordBase

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

    /**
     *  State <-> Avro State
     */

    fun fromStorage(avro: SpecificRecordBase): State = when (avro) {
        is AvroJobEngineState -> fromStorage(avro)
        is AvroMonitoringGlobalState -> fromStorage(avro)
        is AvroMonitoringPerNameState -> fromStorage(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    fun toStorage(state: State): SpecificRecordBase = when (state) {
        is JobEngineState -> toStorage(state)
        is MonitoringGlobalState -> toStorage(state)
        is MonitoringPerNameState -> toStorage(state)
    }

    fun fromStorage(avro: AvroJobEngineState) = convertJson<JobEngineState>(avro)
    fun fromStorage(avro: AvroMonitoringGlobalState) = convertJson<MonitoringGlobalState>(avro)
    fun fromStorage(avro: AvroMonitoringPerNameState) = convertJson<MonitoringPerNameState>(avro)

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

    fun removeEnvelopeFromJobEngineMessage(input: AvroEnvelopeForJobEngine): SpecificRecordBase {
        return when (input.type!!) {
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
        }
    }

    fun addEnvelopeToMonitoringPerNameMessage(message: SpecificRecordBase): AvroEnvelopeForMonitoringPerName {
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

    fun removeEnvelopeFromMonitoringPerNameMessage(input: AvroEnvelopeForMonitoringPerName): SpecificRecordBase {
        return when (input.type) {
            AvroForMonitoringPerNameMessageType.JobStatusUpdated -> input.jobStatusUpdated
            else -> throw Exception("Unknown AvroEnvelopeForMonitoringPerName: ${input::class.qualifiedName}")
        }
    }

    fun addEnvelopeToMonitoringGlobalMessage(message: SpecificRecordBase): AvroEnvelopeForMonitoringGlobal {
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

    fun removeEnvelopeFromMonitoringGlobalMessage(input: AvroEnvelopeForMonitoringGlobal): SpecificRecordBase {
        return when (input.type) {
            AvroForMonitoringGlobalMessageType.JobCreated -> input.jobCreated
            else -> throw Exception("Unknown AvroEnvelopeForMonitoringGlobal: ${input::class.qualifiedName}")
        }
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

    fun removeEnvelopeFromWorkerMessage(input: AvroEnvelopeForWorker): SpecificRecordBase {
        return when (input.type!!) {
            AvroForWorkerMessageType.RunJob -> input.runJob
        }
    }

    /**
     *  Message <-> Avro Envelope
     */

    fun toJobEngine(message: ForJobEngineMessage): AvroEnvelopeForJobEngine =
        addEnvelopeToJobEngineMessage(converToAvro(message))

    fun fromJobEngine(input: AvroEnvelopeForJobEngine) =
        convertFromAvro(removeEnvelopeFromJobEngineMessage(input)) as ForJobEngineMessage

    fun toMonitoringPerName(message: ForMonitoringPerNameMessage): AvroEnvelopeForMonitoringPerName =
        addEnvelopeToMonitoringPerNameMessage(converToAvro(message))

    fun fromMonitoringPerName(input: AvroEnvelopeForMonitoringPerName) =
        convertFromAvro(removeEnvelopeFromMonitoringPerNameMessage(input)) as ForMonitoringPerNameMessage

    fun toMonitoringGlobal(message: ForMonitoringGlobalMessage): AvroEnvelopeForMonitoringGlobal =
        addEnvelopeToMonitoringGlobalMessage(converToAvro(message))

    fun fromMonitoringGlobal(input: AvroEnvelopeForMonitoringGlobal) =
        convertFromAvro(removeEnvelopeFromMonitoringGlobalMessage(input)) as ForMonitoringGlobalMessage

    fun toWorkers(message: ForWorkerMessage): AvroEnvelopeForWorker =
        addEnvelopeToWorkerMessage(converToAvro(message))

    fun fromWorkers(input: AvroEnvelopeForWorker) =
        convertFromAvro(removeEnvelopeFromWorkerMessage(input)) as ForWorkerMessage

    /**
     *  Message <-> Avro Message
     */

    fun convertFromAvro(avro: SpecificRecordBase): Message = when (avro) {
        is AvroCancelJob -> convertFromAvro(avro)
        is AvroDispatchJob -> convertFromAvro(avro)
        is AvroJobAttemptCompleted -> convertFromAvro(avro)
        is AvroJobAttemptDispatched -> convertFromAvro(avro)
        is AvroJobAttemptFailed -> convertFromAvro(avro)
        is AvroJobAttemptStarted -> convertFromAvro(avro)
        is AvroJobCanceled -> convertFromAvro(avro)
        is AvroJobCompleted -> convertFromAvro(avro)
        is AvroJobCreated -> convertFromAvro(avro)
        is AvroJobStatusUpdated -> convertFromAvro(avro)
        is AvroRetryJob -> convertFromAvro(avro)
        is AvroRetryJobAttempt -> convertFromAvro(avro)
        is AvroRunJob -> convertFromAvro(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    fun converToAvro(msg: Message): SpecificRecordBase = when (msg) {
        is CancelJob -> this.convertToAvro(msg)
        is DispatchJob -> this.convertToAvro(msg)
        is JobAttemptCompleted -> this.convertToAvro(msg)
        is JobAttemptDispatched -> this.convertToAvro(msg)
        is JobAttemptFailed -> this.convertToAvro(msg)
        is JobAttemptStarted -> this.convertToAvro(msg)
        is JobCanceled -> this.convertToAvro(msg)
        is JobCompleted -> this.convertToAvro(msg)
        is JobCreated -> this.convertToAvro(msg)
        is JobStatusUpdated -> this.convertToAvro(msg)
        is RetryJob -> this.convertToAvro(msg)
        is RetryJobAttempt -> this.convertToAvro(msg)
        is RunJob -> this.convertToAvro(msg)
    }

    private fun convertFromAvro(avro: AvroCancelJob) = convertJson<CancelJob>(avro)
    private fun convertFromAvro(avro: AvroDispatchJob) = convertJson<DispatchJob>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptCompleted) = convertJson<JobAttemptCompleted>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptDispatched) = convertJson<JobAttemptDispatched>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptFailed) = convertJson<JobAttemptFailed>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptStarted) = convertJson<JobAttemptStarted>(avro)
    private fun convertFromAvro(avro: AvroJobCanceled) = convertJson<JobCanceled>(avro)
    private fun convertFromAvro(avro: AvroJobCompleted) = convertJson<JobCompleted>(avro)
    private fun convertFromAvro(avro: AvroJobCreated) = convertJson<JobCreated>(avro)
    private fun convertFromAvro(avro: AvroJobStatusUpdated) = convertJson<JobStatusUpdated>(avro)
    private fun convertFromAvro(avro: AvroRetryJob) = convertJson<RetryJob>(avro)
    private fun convertFromAvro(avro: AvroRetryJobAttempt) = convertJson<RetryJobAttempt>(avro)
    private fun convertFromAvro(avro: AvroRunJob) = convertJson<RunJob>(avro)

    private fun convertToAvro(message: CancelJob) = convertJson<AvroCancelJob>(message)
    private fun convertToAvro(message: DispatchJob) = convertJson<AvroDispatchJob>(message)
    private fun convertToAvro(message: JobAttemptCompleted) = convertJson<AvroJobAttemptCompleted>(message)
    private fun convertToAvro(message: JobAttemptDispatched) = convertJson<AvroJobAttemptDispatched>(message)
    private fun convertToAvro(message: JobAttemptFailed) = convertJson<AvroJobAttemptFailed>(message)
    private fun convertToAvro(message: JobAttemptStarted) = convertJson<AvroJobAttemptStarted>(message)
    private fun convertToAvro(message: JobCanceled) = convertJson<AvroJobCanceled>(message)
    private fun convertToAvro(message: JobCompleted) = convertJson<AvroJobCompleted>(message)
    private fun convertToAvro(message: JobCreated) = convertJson<AvroJobCreated>(message)
    private fun convertToAvro(message: JobStatusUpdated) = convertJson<AvroJobStatusUpdated>(message)
    private fun convertToAvro(message: RetryJob) = convertJson<AvroRetryJob>(message)
    private fun convertToAvro(message: RetryJobAttempt) = convertJson<AvroRetryJobAttempt>(message)
    private fun convertToAvro(message: RunJob) = convertJson<AvroRunJob>(message)

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convertJson(from: Any): T = Json.parse(Json.stringify(from))
}
