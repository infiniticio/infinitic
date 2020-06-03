package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForEngineMessageType
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptDispatched
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroJobCanceled
import com.zenaton.jobManager.messages.AvroJobCompleted
import com.zenaton.jobManager.messages.AvroJobDispatched
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessageType
import com.zenaton.jobManager.messages.AvroForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerInstanceMessageType
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessageType
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.messages.AvroRetryJobAttempt
import com.zenaton.jobManager.messages.AvroForWorkerMessage
import com.zenaton.jobManager.messages.AvroJobCreated
import com.zenaton.jobManager.messages.AvroJobStatusUpdated
import com.zenaton.jobManager.messages.CancelJob
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.jobManager.messages.JobAttemptCompleted
import com.zenaton.jobManager.messages.JobAttemptDispatched
import com.zenaton.jobManager.messages.JobAttemptFailed
import com.zenaton.jobManager.messages.JobAttemptStarted
import com.zenaton.jobManager.messages.JobCanceled
import com.zenaton.jobManager.messages.JobCompleted
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.JobDispatched
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.RetryJob
import com.zenaton.jobManager.messages.RetryJobAttempt
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalState
import com.zenaton.jobManager.monitoring.perInstance.MonitoringPerInstanceState
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerInstanceState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.jobManager.workers.AvroRunJob
import com.zenaton.jobManager.workers.AvroForWorkerMessageType
import org.apache.avro.specific.SpecificRecordBase
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  States
     */
    fun fromAvro(avro: AvroEngineState) = convert<EngineState>(avro)
    fun fromAvro(avro: AvroMonitoringGlobalState) = convert<MonitoringGlobalState>(avro)
    fun fromAvro(avro: AvroMonitoringPerNameState) = convert<MonitoringPerNameState>(avro)
    fun fromAvro(avro: AvroMonitoringPerInstanceState) = convert<MonitoringPerInstanceState>(avro)

    fun toAvro(state: EngineState) = convert<AvroEngineState>(state)
    fun toAvro(state: MonitoringGlobalState) = convert<AvroMonitoringGlobalState>(state)
    fun toAvro(state: MonitoringPerNameState) = convert<AvroMonitoringPerNameState>(state)
    fun toAvro(state: MonitoringPerInstanceState) = convert<AvroMonitoringPerInstanceState>(state)

    /**
     *  Messages
     */

    fun fromAvro(avro: SpecificRecordBase): Any= when(avro) {
        is AvroCancelJob -> convert<CancelJob>(avro)
        is AvroDispatchJob -> convert<DispatchJob>(avro)
        is AvroJobAttemptCompleted -> convert<JobAttemptCompleted>(avro)
        is AvroJobAttemptDispatched -> convert<JobAttemptDispatched>(avro)
        is AvroJobAttemptFailed -> convert<JobAttemptFailed>(avro)
        is AvroJobAttemptStarted -> convert<JobAttemptStarted>(avro)
        is AvroJobCanceled -> convert<JobCanceled>(avro)
        is AvroJobCompleted -> convert<JobCompleted>(avro)
        is AvroJobCreated -> convert<JobCreated>(avro)
        is AvroJobDispatched -> convert<JobDispatched>(avro)
        is AvroJobStatusUpdated -> convert<JobStatusUpdated>(avro)
        is AvroRetryJob -> convert<RetryJob>(avro)
        is AvroRetryJobAttempt -> convert<RetryJobAttempt>(avro)
        is AvroRunJob -> convert<RunJob>(avro)
        else -> throw Exception("Unknown SpecificRecordBase: $avro")
    }

//    fun toAvro(message: Any): SpecificRecordBase = when(message) {
//        is CancelJob -> convert<AvroCancelJob>(message)
//        is DispatchJob -> convert<AvroDispatchJob>(message)
//        is JobAttemptCompleted -> convert<AvroJobAttemptCompleted>(message)
//        is JobAttemptDispatched -> convert<AvroJobAttemptDispatched>(message)
//        is JobAttemptFailed -> convert<AvroJobAttemptFailed>(message)
//        is JobAttemptStarted -> convert<AvroJobAttemptStarted>(message)
//        is JobCanceled -> convert<AvroJobCanceled>(message)
//        is JobCompleted -> convert<AvroJobCompleted>(message)
//        is JobCreated -> convert<AvroJobCreated>(message)
//        is JobDispatched -> convert<AvroJobDispatched>(message)
//        is JobStatusUpdated -> convert<AvroJobStatusUpdated>(message)
//        is RetryJob -> convert<AvroRetryJob>(message)
//        is RetryJobAttempt -> convert<AvroRetryJobAttempt>(message)
//        is RunJob -> convert<AvroRunJob>(message)
//        else -> throw Exception("Unknown Message: $message")
//    }

    fun toAvro(message: CancelJob) = convert<AvroCancelJob>(message)
    fun toAvro(message: DispatchJob) = convert<AvroDispatchJob>(message)
    fun toAvro(message: JobAttemptCompleted) = convert<AvroJobAttemptCompleted>(message)
    fun toAvro(message: JobAttemptDispatched) = convert<AvroJobAttemptDispatched>(message)
    fun toAvro(message: JobAttemptFailed) = convert<AvroJobAttemptFailed>(message)
    fun toAvro(message: JobAttemptStarted) = convert<AvroJobAttemptStarted>(message)
    fun toAvro(message: JobCanceled) = convert<AvroJobCanceled>(message)
    fun toAvro(message: JobCompleted) = convert<AvroJobCompleted>(message)
    fun toAvro(message: JobCreated) = convert<AvroJobCreated>(message)
    fun toAvro(message: JobDispatched) = convert<AvroJobDispatched>(message)
    fun toAvro(message: JobStatusUpdated) = convert<AvroJobStatusUpdated>(message)
    fun toAvro(message: RetryJob) = convert<AvroRetryJob>(message)
    fun toAvro(message: RetryJobAttempt) = convert<AvroRetryJobAttempt>(message)
    fun toAvro(message: RunJob) = convert<AvroRunJob>(message)

    /**
     *  Envelopes
     */
    fun toAvro(message: ForWorkerMessage): AvroForWorkerMessage {
        val builder = AvroForWorkerMessage.newBuilder()
        when (message) {
            is RunJob -> builder.apply {
                runJob = toAvro(message)
                type = AvroForWorkerMessageType.RunJob
            }
            else -> throw Exception("Unknown WorkerMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForWorkerMessage): ForWorkerMessage {
        return when (val type = input.getType()) {
            AvroForWorkerMessageType.RunJob -> fromAvro(input.runJob)
            else -> throw Exception("Unknown AvroForWorkerMessage: $input")
        } as ForWorkerMessage
    }

    fun toAvro(message: ForMonitoringGlobalMessage): AvroForMonitoringGlobalMessage {
        val builder = AvroForMonitoringGlobalMessage.newBuilder()
        when (message) {
            is JobCreated -> builder.apply {
                jobCreated = toAvro(message)
                type = AvroForMonitoringGlobalMessageType.JobCreated
            }
            else -> throw Exception("Unknown MonitoringGlobalMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForMonitoringGlobalMessage): ForMonitoringGlobalMessage {
        return when (val type = input.getType()) {
            AvroForMonitoringGlobalMessageType.JobCreated -> fromAvro(input.jobCreated)
            else -> throw Exception("Unknown AvroForMonitoringGlobalMessage: $input")
        } as ForMonitoringGlobalMessage
    }

    fun toAvro(message: ForMonitoringPerNameMessage): AvroForMonitoringPerNameMessage {
        val builder = AvroForMonitoringPerNameMessage.newBuilder()
        when (message) {
            is JobStatusUpdated -> builder.apply {
                jobStatusUpdated = toAvro(message)
                type = AvroForMonitoringPerNameMessageType.JobStatusUpdated
            }
            else -> throw Exception("Unknown MonitoringPerNameMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForMonitoringPerNameMessage): ForMonitoringPerNameMessage {
        return when (val type = input.getType()) {
            AvroForMonitoringPerNameMessageType.JobStatusUpdated -> fromAvro(input.jobStatusUpdated)
            else -> throw Exception("Unknown AvroForMonitoringPerNameMessage: $input")
        } as ForMonitoringPerNameMessage
    }

    fun toAvro(message: ForMonitoringPerInstanceMessage): AvroForMonitoringPerInstanceMessage {
        val builder = AvroForMonitoringPerInstanceMessage.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptStarted
            }
            is JobAttemptDispatched -> builder.apply {
                jobAttemptDispatched = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptDispatched
            }
            is JobCanceled -> builder.apply {
                jobCanceled = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.JobCanceled
            }
            is JobCompleted -> builder.apply {
                jobCompleted = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.JobCompleted
            }
            is JobDispatched -> builder.apply {
                jobDispatched = toAvro(message)
                type = AvroForMonitoringPerInstanceMessageType.JobDispatched
            }
            else -> throw Exception("Unknown MonitoringPerInstanceMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForMonitoringPerInstanceMessage): ForMonitoringPerInstanceMessage {
        return when (val type = input.getType()) {
            AvroForMonitoringPerInstanceMessageType.JobAttemptDispatched -> fromAvro(input.jobAttemptDispatched)
            AvroForMonitoringPerInstanceMessageType.JobCanceled -> fromAvro(input.jobCanceled)
            AvroForMonitoringPerInstanceMessageType.JobCompleted -> fromAvro(input.jobCompleted)
            AvroForMonitoringPerInstanceMessageType.JobDispatched -> fromAvro(input.jobDispatched)
            AvroForMonitoringPerInstanceMessageType.CancelJob -> fromAvro(input.cancelJob)
            AvroForMonitoringPerInstanceMessageType.DispatchJob -> fromAvro(input.dispatchJob)
            AvroForMonitoringPerInstanceMessageType.RetryJob -> fromAvro(input.retryJob)
            AvroForMonitoringPerInstanceMessageType.RetryJobAttempt -> fromAvro(input.retryJobAttempt)
            AvroForMonitoringPerInstanceMessageType.JobAttemptCompleted -> fromAvro(input.jobAttemptCompleted)
            AvroForMonitoringPerInstanceMessageType.JobAttemptFailed -> fromAvro(input.jobAttemptFailed)
            AvroForMonitoringPerInstanceMessageType.JobAttemptStarted -> fromAvro(input.jobAttemptStarted)
            else -> throw Exception("Unknown AvroForMonitoringPerInstanceMessage: $input")
        } as ForMonitoringPerInstanceMessage
    }

    /**
     * Engine messages
     */
    fun toAvro(message: ForEngineMessage): AvroForEngineMessage {
        val builder = AvroForEngineMessage.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = toAvro(message)
                type = AvroForEngineMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = toAvro(message)
                type = AvroForEngineMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = toAvro(message)
                type = AvroForEngineMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = toAvro(message)
                type = AvroForEngineMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = toAvro(message)
                type = AvroForEngineMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = toAvro(message)
                type = AvroForEngineMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = toAvro(message)
                type = AvroForEngineMessageType.JobAttemptStarted
            }
            else -> throw Exception("Unknown AvroForMonitoringPerInstanceMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForEngineMessage): ForEngineMessage {
        return when (val type = input.getType()) {
            AvroForEngineMessageType.CancelJob -> fromAvro(input.cancelJob)
            AvroForEngineMessageType.DispatchJob -> fromAvro(input.dispatchJob)
            AvroForEngineMessageType.RetryJob -> fromAvro(input.retryJob)
            AvroForEngineMessageType.RetryJobAttempt -> fromAvro(input.retryJobAttempt)
            AvroForEngineMessageType.JobAttemptCompleted -> fromAvro(input.jobAttemptCompleted)
            AvroForEngineMessageType.JobAttemptFailed -> fromAvro(input.jobAttemptFailed)
            AvroForEngineMessageType.JobAttemptStarted -> fromAvro(input.jobAttemptStarted)
            else -> throw Exception("Unknown AvroForEngineMessage: $input")
        } as ForEngineMessage
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse(Json.stringify(from))
}
