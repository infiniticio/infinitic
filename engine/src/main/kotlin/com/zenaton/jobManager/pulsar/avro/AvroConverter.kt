package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.json.Json
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

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  States
     */
    fun toAvro(state: EngineState) = convert<AvroEngineState>(state)
    fun fromAvro(avroState: AvroEngineState) = convert<EngineState>(avroState)

    fun toAvro(state: MonitoringGlobalState) = convert<AvroMonitoringGlobalState>(state)
    fun fromAvro(avroState: AvroMonitoringGlobalState) = convert<MonitoringGlobalState>(avroState)

    fun toAvro(state: MonitoringPerNameState) = convert<AvroMonitoringPerNameState>(state)
    fun fromAvro(avroState: AvroMonitoringPerNameState) = convert<MonitoringPerNameState>(avroState)

    fun toAvro(state: MonitoringPerInstanceState) = convert<AvroMonitoringPerInstanceState>(state)
    fun fromAvro(avroState: AvroMonitoringPerInstanceState) = convert<MonitoringPerInstanceState>(avroState)

    /**
     *  Messages
     */
    fun toAvro(message: ForWorkerMessage): AvroForWorkerMessage {
        val builder = AvroForWorkerMessage.newBuilder()
        when (message) {
            is RunJob -> builder.apply {
                runJob = convert<AvroRunJob>(message)
                type = AvroForWorkerMessageType.RunJob
            }
            else -> throw Exception("Unknown WorkerMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForWorkerMessage): ForWorkerMessage {
        return when (val type = input.getType()) {
            AvroForWorkerMessageType.RunJob -> convert<RunJob>(input.runJob)
            else -> throw Exception("Unknown AvroForWorkerMessage: $input")
        }
    }

    fun toAvro(message: ForMonitoringGlobalMessage): AvroForMonitoringGlobalMessage {
        val builder = AvroForMonitoringGlobalMessage.newBuilder()
        when (message) {
            is JobCreated -> builder.apply {
                jobCreated = convert(message)
                type = AvroForMonitoringGlobalMessageType.JobCreated
            }
            else -> throw Exception("Unknown MonitoringGlobalMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForMonitoringGlobalMessage): ForMonitoringGlobalMessage {
        return when (val type = input.getType()) {
            AvroForMonitoringGlobalMessageType.JobCreated -> convert<JobCreated>(input.jobCreated)
            else -> throw Exception("Unknown AvroForMonitoringGlobalMessage: $input")
        }
    }

    fun toAvro(message: ForMonitoringPerNameMessage): AvroForMonitoringPerNameMessage {
        val builder = AvroForMonitoringPerNameMessage.newBuilder()
        when (message) {
            is JobStatusUpdated -> builder.apply {
                jobStatusUpdated = convert(message)
                type = AvroForMonitoringPerNameMessageType.JobStatusUpdated
            }
            else -> throw Exception("Unknown MonitoringPerNameMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForMonitoringPerNameMessage): ForMonitoringPerNameMessage {
        return when (val type = input.getType()) {
            AvroForMonitoringPerNameMessageType.JobStatusUpdated -> convert<JobStatusUpdated>(input.jobStatusUpdated)
            else -> throw Exception("Unknown AvroForMonitoringPerNameMessage: $input")
        }
    }

    fun toAvro(message: ForMonitoringPerInstanceMessage): AvroForMonitoringPerInstanceMessage {
        val builder = AvroForMonitoringPerInstanceMessage.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = convert<AvroCancelJob>(message)
                type = AvroForMonitoringPerInstanceMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convert<AvroDispatchJob>(message)
                type = AvroForMonitoringPerInstanceMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convert<AvroRetryJob>(message)
                type = AvroForMonitoringPerInstanceMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convert<AvroRetryJobAttempt>(message)
                type = AvroForMonitoringPerInstanceMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convert<AvroJobAttemptCompleted>(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convert<AvroJobAttemptFailed>(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convert<AvroJobAttemptStarted>(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptStarted
            }
            is JobAttemptDispatched -> builder.apply {
                jobAttemptDispatched = convert<AvroJobAttemptDispatched>(message)
                type = AvroForMonitoringPerInstanceMessageType.JobAttemptDispatched
            }
            is JobCanceled -> builder.apply {
                jobCanceled = convert<AvroJobCanceled>(message)
                type = AvroForMonitoringPerInstanceMessageType.JobCanceled
            }
            is JobCompleted -> builder.apply {
                jobCompleted = convert<AvroJobCompleted>(message)
                type = AvroForMonitoringPerInstanceMessageType.JobCompleted
            }
            is JobDispatched -> builder.apply {
                jobDispatched = convert<AvroJobDispatched>(message)
                type = AvroForMonitoringPerInstanceMessageType.JobDispatched
            }
            else -> throw Exception("Unknown MonitoringPerInstanceMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForMonitoringPerInstanceMessage): ForMonitoringPerInstanceMessage {
        return when (val type = input.getType()) {
            AvroForMonitoringPerInstanceMessageType.JobAttemptDispatched -> convert<JobAttemptDispatched>(input.jobAttemptDispatched)
            AvroForMonitoringPerInstanceMessageType.JobCanceled -> convert<JobCanceled>(input.jobCanceled)
            AvroForMonitoringPerInstanceMessageType.JobCompleted -> convert<JobCompleted>(input.jobCompleted)
            AvroForMonitoringPerInstanceMessageType.JobDispatched -> convert<JobDispatched>(input.jobDispatched)
            AvroForMonitoringPerInstanceMessageType.CancelJob -> convert<CancelJob>(input.cancelJob)
            AvroForMonitoringPerInstanceMessageType.DispatchJob -> convert<DispatchJob>(input.dispatchJob)
            AvroForMonitoringPerInstanceMessageType.RetryJob -> convert<RetryJob>(input.retryJob)
            AvroForMonitoringPerInstanceMessageType.RetryJobAttempt -> convert<RetryJobAttempt>(input.retryJobAttempt)
            AvroForMonitoringPerInstanceMessageType.JobAttemptCompleted -> convert<JobAttemptCompleted>(input.jobAttemptCompleted)
            AvroForMonitoringPerInstanceMessageType.JobAttemptFailed -> convert<JobAttemptFailed>(input.jobAttemptFailed)
            AvroForMonitoringPerInstanceMessageType.JobAttemptStarted -> convert<JobAttemptStarted>(input.jobAttemptStarted)
            else -> throw Exception("Unknown AvroForMonitoringPerInstanceMessage: $input")
        }
    }

    /**
     * Engine messages
     */
    fun toAvro(message: ForEngineMessage): AvroForEngineMessage {
        val builder = AvroForEngineMessage.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = convert<AvroCancelJob>(message)
                type = AvroForEngineMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convert<AvroDispatchJob>(message)
                type = AvroForEngineMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convert<AvroRetryJob>(message)
                type = AvroForEngineMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convert<AvroRetryJobAttempt>(message)
                type = AvroForEngineMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convert<AvroJobAttemptCompleted>(message)
                type = AvroForEngineMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convert<AvroJobAttemptFailed>(message)
                type = AvroForEngineMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convert<AvroJobAttemptStarted>(message)
                type = AvroForEngineMessageType.JobAttemptStarted
            }
            else -> throw Exception("Unknown AvroForMonitoringPerInstanceMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroForEngineMessage): ForEngineMessage {
        return when (val type = input.getType()) {
            AvroForEngineMessageType.CancelJob -> convert<CancelJob>(input.cancelJob)
            AvroForEngineMessageType.DispatchJob -> convert<DispatchJob>(input.dispatchJob)
            AvroForEngineMessageType.RetryJob -> convert<RetryJob>(input.retryJob)
            AvroForEngineMessageType.RetryJobAttempt -> convert<RetryJobAttempt>(input.retryJobAttempt)
            AvroForEngineMessageType.JobAttemptCompleted -> convert<JobAttemptCompleted>(input.jobAttemptCompleted)
            AvroForEngineMessageType.JobAttemptFailed -> convert<JobAttemptFailed>(input.jobAttemptFailed)
            AvroForEngineMessageType.JobAttemptStarted -> convert<JobAttemptStarted>(input.jobAttemptStarted)
            else -> throw Exception("Unknown AvroForEngineMessage: $input")
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse(Json.stringify(from))
}
