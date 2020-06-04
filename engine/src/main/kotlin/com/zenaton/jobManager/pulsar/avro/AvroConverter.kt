package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForEngineMessageType
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessageType
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessageType
import com.zenaton.jobManager.messages.AvroForWorkerMessage
import com.zenaton.jobManager.messages.AvroForWorkerMessageType
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
import com.zenaton.jobManager.messages.JobAttemptCompleted
import com.zenaton.jobManager.messages.JobAttemptDispatched
import com.zenaton.jobManager.messages.JobAttemptFailed
import com.zenaton.jobManager.messages.JobAttemptStarted
import com.zenaton.jobManager.messages.JobCanceled
import com.zenaton.jobManager.messages.JobCompleted
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.RetryJob
import com.zenaton.jobManager.messages.RetryJobAttempt
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalState
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import org.apache.avro.specific.SpecificRecordBase

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

    /**
     *  States
     */
    fun fromAvro(avro: AvroEngineState) = convertJson<EngineState>(avro)
    fun fromAvro(avro: AvroMonitoringGlobalState) = convertJson<MonitoringGlobalState>(avro)
    fun fromAvro(avro: AvroMonitoringPerNameState) = convertJson<MonitoringPerNameState>(avro)

    fun toAvro(state: EngineState) = convertJson<AvroEngineState>(state)
    fun toAvro(state: MonitoringGlobalState) = convertJson<AvroMonitoringGlobalState>(state)
    fun toAvro(state: MonitoringPerNameState) = convertJson<AvroMonitoringPerNameState>(state)

    /**
     *  Envelopes
     */
    fun toAvroForWorkerMessage(message: ForWorkerMessage): AvroForWorkerMessage {
        val builder = AvroForWorkerMessage.newBuilder()
        when (message) {
            is RunJob -> builder.apply {
                runJob = convertToAvro(message)
                type = AvroForWorkerMessageType.RunJob
            }
            else -> throw Exception("Unknown ForWorkerMessage: $message")
        }
        return builder.build()
    }

    fun fromAvroForWorkerMessage(input: AvroForWorkerMessage): ForWorkerMessage {
        return when (input.getType()) {
            AvroForWorkerMessageType.RunJob -> convertFromAvro(input.runJob)
            else -> throw Exception("Unknown AvroForWorkerMessage: $input")
        }
    }

    fun toAvroForMonitoringGlobalMessage(message: ForMonitoringGlobalMessage): AvroForMonitoringGlobalMessage {
        val builder = AvroForMonitoringGlobalMessage.newBuilder()
        when (message) {
            is JobCreated -> builder.apply {
                jobCreated = convertToAvro(message)
                type = AvroForMonitoringGlobalMessageType.JobCreated
            }
            else -> throw Exception("Unknown ForMonitoringGlobalMessage: $message")
        }
        return builder.build()
    }

    fun fromAvroForMonitoringGlobalMessage(input: AvroForMonitoringGlobalMessage): ForMonitoringGlobalMessage {
        return when (input.getType()) {
            AvroForMonitoringGlobalMessageType.JobCreated -> convertFromAvro(input.jobCreated)
            else -> throw Exception("Unknown AvroForMonitoringGlobalMessage: $input")
        }
    }

    fun toAvroForMonitoringPerNameMessage(message: ForMonitoringPerNameMessage): AvroForMonitoringPerNameMessage {
        val builder = AvroForMonitoringPerNameMessage.newBuilder()
        when (message) {
            is JobStatusUpdated -> builder.apply {
                jobStatusUpdated = convertToAvro(message)
                type = AvroForMonitoringPerNameMessageType.JobStatusUpdated
            }
            else -> throw Exception("Unknown ForMonitoringPerNameMessage: $message")
        }
        return builder.build()
    }

    fun fromAvroForMonitoringPerNameMessage(input: AvroForMonitoringPerNameMessage): ForMonitoringPerNameMessage {
        return when (input.getType()) {
            AvroForMonitoringPerNameMessageType.JobStatusUpdated -> convertFromAvro(input.jobStatusUpdated)
            else -> throw Exception("Unknown AvroForMonitoringPerNameMessage: $input")
        }
    }

    fun toAvroForEngineMessage(message: ForEngineMessage): AvroForEngineMessage {
        val builder = AvroForEngineMessage.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = convertToAvro(message)
                type = AvroForEngineMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convertToAvro(message)
                type = AvroForEngineMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convertToAvro(message)
                type = AvroForEngineMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convertToAvro(message)
                type = AvroForEngineMessageType.RetryJobAttempt
            }
            is JobAttemptDispatched -> builder.apply {
                jobAttemptDispatched = convertToAvro(message)
                type = AvroForEngineMessageType.JobAttemptDispatched
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convertToAvro(message)
                type = AvroForEngineMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convertToAvro(message)
                type = AvroForEngineMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convertToAvro(message)
                type = AvroForEngineMessageType.JobAttemptStarted
            }
            is JobCanceled -> builder.apply {
                jobCanceled = convertToAvro(message)
                type = AvroForEngineMessageType.JobCanceled
            }
            is JobCompleted -> builder.apply {
                jobCompleted = convertToAvro(message)
                type = AvroForEngineMessageType.JobCompleted
            }
            else -> throw Exception("Unknown ForJobEngineMessage: $message")
        }
        return builder.build()
    }

    fun fromAvroForEngineMessage(input: AvroForEngineMessage): ForEngineMessage {
        return when (input.getType()) {
            AvroForEngineMessageType.CancelJob -> convertFromAvro(input.cancelJob)
            AvroForEngineMessageType.DispatchJob -> convertFromAvro(input.dispatchJob)
            AvroForEngineMessageType.RetryJob -> convertFromAvro(input.retryJob)
            AvroForEngineMessageType.RetryJobAttempt -> convertFromAvro(input.retryJobAttempt)
            AvroForEngineMessageType.JobAttemptDispatched -> convertFromAvro(input.jobAttemptDispatched)
            AvroForEngineMessageType.JobAttemptCompleted -> convertFromAvro(input.jobAttemptCompleted)
            AvroForEngineMessageType.JobAttemptFailed -> convertFromAvro(input.jobAttemptFailed)
            AvroForEngineMessageType.JobAttemptStarted -> convertFromAvro(input.jobAttemptStarted)
            AvroForEngineMessageType.JobCanceled -> convertFromAvro(input.jobCanceled)
            AvroForEngineMessageType.JobCompleted -> convertFromAvro(input.jobCompleted)
            else -> throw Exception("Unknown AvroForEngineMessage: $input")
        }
    }

    /**
     *  Messages
     */

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
     *  Any Message
     */

    fun convertFromAvro(avro: SpecificRecordBase): Any = when (avro) {
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
        else -> throw Exception("Unknown SpecificRecordBase: $avro")
    }

//    fun convertToAvro(message: Message): SpecificRecordBase = when (message) {
//        is CancelJob -> convertToAvro(message)
//        is DispatchJob -> convertToAvro(message)
//        is JobAttemptCompleted -> convertToAvro(message)
//        is JobAttemptDispatched -> convertToAvro(message)
//        is JobAttemptFailed -> convertToAvro(message)
//        is JobAttemptStarted -> convertToAvro(message)
//        is JobCanceled -> convertToAvro(message)
//        is JobCompleted -> convertToAvro(message)
//        is JobCreated -> convertToAvro(message)
//        is JobDispatched -> convertToAvro(message)
//        is JobStatusUpdated -> convertToAvro(message)
//        is RetryJob -> convertToAvro(message)
//        is RetryJobAttempt -> convertToAvro(message)
//        is RunJob -> convertToAvro(message)
//    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convertJson(from: Any): T = Json.parse(Json.stringify(from))
}
