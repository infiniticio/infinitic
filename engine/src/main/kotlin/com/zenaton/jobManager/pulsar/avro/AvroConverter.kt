package com.zenaton.jobManager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.jobManager.messages.monitoring.global.AvroMonitoringGlobalMessage
import com.zenaton.jobManager.messages.monitoring.global.AvroMonitoringGlobalMessageType
import com.zenaton.jobManager.engine.CancelJob
import com.zenaton.jobManager.engine.DispatchJob
import com.zenaton.jobManager.engine.EngineMessage
import com.zenaton.jobManager.engine.EngineState
import com.zenaton.jobManager.engine.JobAttemptCompleted
import com.zenaton.jobManager.engine.JobAttemptFailed
import com.zenaton.jobManager.engine.JobAttemptStarted
import com.zenaton.jobManager.engine.RetryJob
import com.zenaton.jobManager.engine.RetryJobAttempt
import com.zenaton.jobManager.messages.engine.AvroCancelJob
import com.zenaton.jobManager.messages.engine.AvroDispatchJob
import com.zenaton.jobManager.messages.engine.AvroEngineMessage
import com.zenaton.jobManager.messages.engine.AvroEngineMessageType
import com.zenaton.jobManager.messages.engine.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.engine.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.engine.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.engine.AvroRetryJob
import com.zenaton.jobManager.messages.engine.AvroRetryJobAttempt
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobAttemptDispatched
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobCanceled
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobCompleted
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroJobDispatched
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.monitoring.perInstance.AvroMonitoringPerInstanceMessageType
import com.zenaton.jobManager.messages.monitoring.perName.AvroMonitoringPerNameMessage
import com.zenaton.jobManager.messages.monitoring.perName.AvroMonitoringPerNameMessageType
import com.zenaton.jobManager.monitoring.global.JobCreated
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalState
import com.zenaton.jobManager.monitoring.perInstance.CancelJob as MonitoringCancelJob
import com.zenaton.jobManager.monitoring.perInstance.DispatchJob as MonitoringDispatchJob
import com.zenaton.jobManager.monitoring.perInstance.JobAttemptCompleted as MonitoringJobAttemptCompleted
import com.zenaton.jobManager.monitoring.perInstance.JobAttemptFailed as MonitoringJobAttemptFailed
import com.zenaton.jobManager.monitoring.perInstance.JobAttemptStarted as MonitoringJobAttemptStarted
import com.zenaton.jobManager.monitoring.perInstance.RetryJob as MonitoringRetryJob
import com.zenaton.jobManager.monitoring.perInstance.RetryJobAttempt as MonitoringRetryJobAttempt
import com.zenaton.jobManager.monitoring.perInstance.JobAttemptDispatched
import com.zenaton.jobManager.monitoring.perInstance.JobCanceled
import com.zenaton.jobManager.monitoring.perInstance.JobCompleted
import com.zenaton.jobManager.monitoring.perInstance.JobDispatched
import com.zenaton.jobManager.monitoring.perInstance.MonitoringPerInstanceMessage
import com.zenaton.jobManager.monitoring.perInstance.MonitoringPerInstanceState
import com.zenaton.jobManager.monitoring.perName.JobStatusUpdated
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerInstanceState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.jobManager.workers.AvroRunJob
import com.zenaton.jobManager.workers.AvroWorkerMessage
import com.zenaton.jobManager.workers.AvroWorkerMessageType
import com.zenaton.jobManager.workers.RunJob
import com.zenaton.jobManager.workers.WorkerMessage

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  Worker State
     */
    fun toAvro(obj: EngineState) = convert<AvroEngineState>(obj)
    fun fromAvro(obj: AvroEngineState) = convert<EngineState>(obj)

    /**
     *  Worker messages
     */
    fun toAvro(msg: WorkerMessage): AvroWorkerMessage {
        val builder = AvroWorkerMessage.newBuilder()
        when (msg) {
            is RunJob -> builder.apply {
                runJob = convert<AvroRunJob>(msg)
                type = AvroWorkerMessageType.RunJob
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroWorkerMessage): WorkerMessage {
        return when (val type = input.getType()) {
            AvroWorkerMessageType.RunJob -> convert<RunJob>(input.runJob)
        }
    }

    /**
     * Monitoring Global state
     */
    fun toAvro(obj: MonitoringGlobalState) = convert<AvroMonitoringGlobalState>(obj)
    fun fromAvro(obj: AvroMonitoringGlobalState) = convert<MonitoringGlobalState>(obj)

    /**
     * Monitoring Global messages
     */
    fun toAvro(msg: MonitoringGlobalMessage): AvroMonitoringGlobalMessage {
        val builder = AvroMonitoringGlobalMessage.newBuilder()
        when (msg) {
            is JobCreated -> builder.apply {
                jobCreated = convert(msg)
                type = AvroMonitoringGlobalMessageType.JobCreated
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringGlobalMessage): MonitoringGlobalMessage {
        return when (val type = input.getType()) {
            AvroMonitoringGlobalMessageType.JobCreated -> convert<JobCreated>(input.jobCreated)
        }
    }

    /**
     * Monitoring Per Name state
     */
    fun toAvro(obj: MonitoringPerNameState) = convert<AvroMonitoringPerNameState>(obj)
    fun fromAvro(obj: AvroMonitoringPerNameState) = convert<MonitoringPerNameState>(obj)

    /**
     * Monitoring Per Name messages
     */
    fun toAvro(msg: MonitoringPerNameMessage): AvroMonitoringPerNameMessage {
        val builder = AvroMonitoringPerNameMessage.newBuilder()
        when (msg) {
            is JobStatusUpdated -> builder.apply {
                jobStatusUpdated = convert(msg)
                type = AvroMonitoringPerNameMessageType.JobStatusUpdated
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringPerNameMessage): MonitoringPerNameMessage {
        return when (val type = input.getType()) {
            AvroMonitoringPerNameMessageType.JobStatusUpdated -> convert<JobStatusUpdated>(input.jobStatusUpdated)
        }
    }

    /**
     * Monitoring Per Instance state
     */
    fun toAvro(obj: MonitoringPerInstanceState) = convert<AvroMonitoringPerInstanceState>(obj)
    fun fromAvro(obj: AvroMonitoringPerInstanceState) = convert<MonitoringPerInstanceState>(obj)

    /**
     * Monitoring Per Name messages
     */
    fun toAvro(msg: MonitoringPerInstanceMessage): AvroMonitoringPerInstanceMessage {
        val builder = AvroMonitoringPerInstanceMessage.newBuilder()
        builder.jobId = msg.jobId.id
        when (msg) {
            is JobAttemptDispatched -> builder.apply {
                jobAttemptDispatched = convert<AvroJobAttemptDispatched>(msg)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptDispatched
            }
            is JobCanceled -> builder.apply {
                jobCanceled = convert<AvroJobCanceled>(msg)
                type = AvroMonitoringPerInstanceMessageType.JobCanceled
            }
            is JobCompleted -> builder.apply {
                jobCompleted = convert<AvroJobCompleted>(msg)
                type = AvroMonitoringPerInstanceMessageType.JobCompleted
            }
            is JobDispatched -> builder.apply {
                jobDispatched = convert<AvroJobDispatched>(msg)
                type = AvroMonitoringPerInstanceMessageType.JobDispatched
            }
        }
        return builder.build()
    }


    fun toMonitoringPerInstanceAvro(msg: EngineMessage): AvroMonitoringPerInstanceMessage {
        val builder = AvroMonitoringPerInstanceMessage.newBuilder()
        builder.jobId = msg.jobId.id
        when (msg) {
            is CancelJob -> builder.apply {
                cancelJob = convert<AvroCancelJob>(msg)
                type = AvroMonitoringPerInstanceMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convert<AvroDispatchJob>(msg)
                type = AvroMonitoringPerInstanceMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convert<AvroRetryJob>(msg)
                type = AvroMonitoringPerInstanceMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convert<AvroRetryJobAttempt>(msg)
                type = AvroMonitoringPerInstanceMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convert<AvroJobAttemptCompleted>(msg)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convert<AvroJobAttemptFailed>(msg)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convert<AvroJobAttemptStarted>(msg)
                type = AvroMonitoringPerInstanceMessageType.JobAttemptStarted
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringPerInstanceMessage): MonitoringPerInstanceMessage {
        return when (val type = input.getType()) {
            AvroMonitoringPerInstanceMessageType.JobAttemptDispatched -> convert<JobAttemptDispatched>(input.jobAttemptDispatched)
            AvroMonitoringPerInstanceMessageType.JobCanceled -> convert<JobCanceled>(input.jobCanceled)
            AvroMonitoringPerInstanceMessageType.JobCompleted -> convert<JobCompleted>(input.jobCompleted)
            AvroMonitoringPerInstanceMessageType.JobDispatched -> convert<JobDispatched>(input.jobDispatched)
            AvroMonitoringPerInstanceMessageType.CancelJob -> convert<MonitoringCancelJob>(input.cancelJob)
            AvroMonitoringPerInstanceMessageType.DispatchJob -> convert<MonitoringDispatchJob>(input.dispatchJob)
            AvroMonitoringPerInstanceMessageType.RetryJob -> convert<MonitoringRetryJob>(input.retryJob)
            AvroMonitoringPerInstanceMessageType.RetryJobAttempt -> convert<MonitoringRetryJobAttempt>(input.retryJobAttempt)
            AvroMonitoringPerInstanceMessageType.JobAttemptCompleted -> convert<MonitoringJobAttemptCompleted>(input.jobAttemptCompleted)
            AvroMonitoringPerInstanceMessageType.JobAttemptFailed -> convert<MonitoringJobAttemptFailed>(input.jobAttemptFailed)
            AvroMonitoringPerInstanceMessageType.JobAttemptStarted -> convert<MonitoringJobAttemptStarted>(input.jobAttemptStarted)
        }
    }

    /**
     * Engine messages
     */
    fun toAvro(msg: EngineMessage): AvroEngineMessage {
        val builder = AvroEngineMessage.newBuilder()
        builder.jobId = msg.jobId.id
        when (msg) {
            is CancelJob -> builder.apply {
                cancelJob = convert<AvroCancelJob>(msg)
                type = AvroEngineMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convert<AvroDispatchJob>(msg)
                type = AvroEngineMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convert<AvroRetryJob>(msg)
                type = AvroEngineMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convert<AvroRetryJobAttempt>(msg)
                type = AvroEngineMessageType.RetryJobAttempt
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convert<AvroJobAttemptCompleted>(msg)
                type = AvroEngineMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convert<AvroJobAttemptFailed>(msg)
                type = AvroEngineMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convert<AvroJobAttemptStarted>(msg)
                type = AvroEngineMessageType.JobAttemptStarted
            }
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
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse(Json.stringify(from))
}
