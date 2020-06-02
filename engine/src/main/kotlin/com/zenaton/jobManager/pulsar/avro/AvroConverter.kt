package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroEngineMessage
import com.zenaton.jobManager.messages.AvroEngineMessageType
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptDispatched
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroJobCanceled
import com.zenaton.jobManager.messages.AvroJobCompleted
import com.zenaton.jobManager.messages.AvroJobDispatched
import com.zenaton.jobManager.messages.AvroMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroMonitoringGlobalMessageType
import com.zenaton.jobManager.messages.AvroMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.AvroMonitoringPerInstanceMessageType
import com.zenaton.jobManager.messages.AvroMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroMonitoringPerNameMessageType
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.messages.AvroRetryJobAttempt
import com.zenaton.jobManager.messages.AvroWorkerMessage
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
import com.zenaton.jobManager.messages.interfaces.EngineMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.WorkerMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalState
import com.zenaton.jobManager.monitoring.perInstance.MonitoringPerInstanceState
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerInstanceState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.jobManager.workers.AvroRunJob
import com.zenaton.jobManager.workers.AvroWorkerMessageType

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
    fun toAvro(message: WorkerMessage): AvroWorkerMessage {
        val builder = AvroWorkerMessage.newBuilder()
        when (message) {
            is RunJob -> builder.apply {
                runJob = convert<AvroRunJob>(message)
                type = AvroWorkerMessageType.RunJob
            }
            else -> throw Exception("Unknown WorkerMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroWorkerMessage): WorkerMessage {
        return when (val type = input.getType()) {
            AvroWorkerMessageType.RunJob -> convert<RunJob>(input.runJob)
            else -> throw Exception("Unknown AvroWorkerMessage: $input")
        }
    }

    fun toAvro(message: MonitoringGlobalMessage): AvroMonitoringGlobalMessage {
        val builder = AvroMonitoringGlobalMessage.newBuilder()
        when (message) {
            is JobCreated -> builder.apply {
                jobCreated = convert(message)
                type = AvroMonitoringGlobalMessageType.JobCreated
            }
            else -> throw Exception("Unknown MonitoringGlobalMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringGlobalMessage): MonitoringGlobalMessage {
        return when (val type = input.getType()) {
            AvroMonitoringGlobalMessageType.JobCreated -> convert<JobCreated>(input.jobCreated)
            else -> throw Exception("Unknown AvroMonitoringGlobalMessage: $input")
        }
    }

    fun toAvro(message: MonitoringPerNameMessage): AvroMonitoringPerNameMessage {
        val builder = AvroMonitoringPerNameMessage.newBuilder()
        when (message) {
            is JobStatusUpdated -> builder.apply {
                jobStatusUpdated = convert(message)
                type = AvroMonitoringPerNameMessageType.JobStatusUpdated
            }
            else -> throw Exception("Unknown MonitoringPerNameMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringPerNameMessage): MonitoringPerNameMessage {
        return when (val type = input.getType()) {
            AvroMonitoringPerNameMessageType.JobStatusUpdated -> convert<JobStatusUpdated>(input.jobStatusUpdated)
            else -> throw Exception("Unknown AvroMonitoringPerNameMessage: $input")
        }
    }

    fun toAvro(message: MonitoringPerInstanceMessage): AvroMonitoringPerInstanceMessage {
        val builder = AvroMonitoringPerInstanceMessage.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = convert<AvroCancelJob>(message)
                type = AvroMonitoringPerInstanceMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convert<AvroDispatchJob>(message)
                type = AvroMonitoringPerInstanceMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convert<AvroRetryJob>(message)
                type = AvroMonitoringPerInstanceMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convert<AvroRetryJobAttempt>(message)
                type = AvroMonitoringPerInstanceMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convert<AvroJobAttemptCompleted>(message)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convert<AvroJobAttemptFailed>(message)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convert<AvroJobAttemptStarted>(message)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptStarted
            }
            is JobAttemptDispatched -> builder.apply {
                jobAttemptDispatched = convert<AvroJobAttemptDispatched>(message)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptDispatched
            }
            is JobCanceled -> builder.apply {
                jobCanceled = convert<AvroJobCanceled>(message)
                type = AvroMonitoringPerInstanceMessageType.JobCanceled
            }
            is JobCompleted -> builder.apply {
                jobCompleted = convert<AvroJobCompleted>(message)
                type = AvroMonitoringPerInstanceMessageType.JobCompleted
            }
            is JobDispatched -> builder.apply {
                jobDispatched = convert<AvroJobDispatched>(message)
                type = AvroMonitoringPerInstanceMessageType.JobDispatched
            }
            else -> throw Exception("Unknown MonitoringPerInstanceMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringPerInstanceMessage): MonitoringPerInstanceMessage {
        return when (val type = input.getType()) {
            AvroMonitoringPerInstanceMessageType.JobAttemptDispatched -> convert<JobAttemptDispatched>(input.jobAttemptDispatched)
            AvroMonitoringPerInstanceMessageType.JobCanceled -> convert<JobCanceled>(input.jobCanceled)
            AvroMonitoringPerInstanceMessageType.JobCompleted -> convert<JobCompleted>(input.jobCompleted)
            AvroMonitoringPerInstanceMessageType.JobDispatched -> convert<JobDispatched>(input.jobDispatched)
            AvroMonitoringPerInstanceMessageType.CancelJob -> convert<CancelJob>(input.cancelJob)
            AvroMonitoringPerInstanceMessageType.DispatchJob -> convert<DispatchJob>(input.dispatchJob)
            AvroMonitoringPerInstanceMessageType.RetryJob -> convert<RetryJob>(input.retryJob)
            AvroMonitoringPerInstanceMessageType.RetryJobAttempt -> convert<RetryJobAttempt>(input.retryJobAttempt)
            AvroMonitoringPerInstanceMessageType.JobAttemptCompleted -> convert<JobAttemptCompleted>(input.jobAttemptCompleted)
            AvroMonitoringPerInstanceMessageType.JobAttemptFailed -> convert<JobAttemptFailed>(input.jobAttemptFailed)
            AvroMonitoringPerInstanceMessageType.JobAttemptStarted -> convert<JobAttemptStarted>(input.jobAttemptStarted)
            else -> throw Exception("Unknown AvroMonitoringPerInstanceMessage: $input")
        }
    }

    /**
     * Engine messages
     */
    fun toAvro(message: EngineMessage): AvroEngineMessage {
        val builder = AvroEngineMessage.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = convert<AvroCancelJob>(message)
                type = AvroEngineMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convert<AvroDispatchJob>(message)
                type = AvroEngineMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convert<AvroRetryJob>(message)
                type = AvroEngineMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convert<AvroRetryJobAttempt>(message)
                type = AvroEngineMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convert<AvroJobAttemptCompleted>(message)
                type = AvroEngineMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convert<AvroJobAttemptFailed>(message)
                type = AvroEngineMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convert<AvroJobAttemptStarted>(message)
                type = AvroEngineMessageType.JobAttemptStarted
            }
            else -> throw Exception("Unknown AvroMonitoringPerInstanceMessage: $message")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroEngineMessage): EngineMessage {
        return when (val type = input.getType()) {
            AvroEngineMessageType.CancelJob -> convert<CancelJob>(input.cancelJob)
            AvroEngineMessageType.DispatchJob -> convert<DispatchJob>(input.dispatchJob)
            AvroEngineMessageType.RetryJob -> convert<RetryJob>(input.retryJob)
            AvroEngineMessageType.RetryJobAttempt -> convert<RetryJobAttempt>(input.retryJobAttempt)
            AvroEngineMessageType.JobAttemptCompleted -> convert<JobAttemptCompleted>(input.jobAttemptCompleted)
            AvroEngineMessageType.JobAttemptFailed -> convert<JobAttemptFailed>(input.jobAttemptFailed)
            AvroEngineMessageType.JobAttemptStarted -> convert<JobAttemptStarted>(input.jobAttemptStarted)
            else -> throw Exception("Unknown AvroEngineMessage: $input")
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse(Json.stringify(from))
}
