package com.zenaton.taskManager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.jobManager.admin.messages.AvroMonitoringGlobalMessage
import com.zenaton.jobManager.admin.messages.AvroMonitoringGlobalMessageType
import com.zenaton.jobManager.engine.messages.AvroCancelJob
import com.zenaton.jobManager.engine.messages.AvroDispatchJob
import com.zenaton.jobManager.engine.messages.AvroEngineMessage
import com.zenaton.jobManager.engine.messages.AvroEngineMessageType
import com.zenaton.jobManager.engine.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.engine.messages.AvroJobAttemptDispatched
import com.zenaton.jobManager.engine.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.engine.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.engine.messages.AvroJobCanceled
import com.zenaton.jobManager.engine.messages.AvroJobCompleted
import com.zenaton.jobManager.engine.messages.AvroJobDispatched
import com.zenaton.jobManager.engine.messages.AvroRetryJob
import com.zenaton.jobManager.engine.messages.AvroRetryJobAttempt
import com.zenaton.jobManager.metrics.messages.AvroMonitoringPerNameMessage
import com.zenaton.jobManager.metrics.messages.AvroMonitoringPerNameMessageType
import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.jobManager.workers.AvroRunJob
import com.zenaton.jobManager.workers.AvroWorkerMessage
import com.zenaton.jobManager.workers.AvroWorkerMessageType
import com.zenaton.taskManager.engine.CancelJob
import com.zenaton.taskManager.engine.DispatchJob
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.engine.EngineState
import com.zenaton.taskManager.engine.JobAttemptCompleted
import com.zenaton.taskManager.engine.JobAttemptDispatched
import com.zenaton.taskManager.engine.JobAttemptFailed
import com.zenaton.taskManager.engine.JobAttemptStarted
import com.zenaton.taskManager.engine.JobCanceled
import com.zenaton.taskManager.engine.JobCompleted
import com.zenaton.taskManager.engine.JobDispatched
import com.zenaton.taskManager.engine.RetryJob
import com.zenaton.taskManager.engine.RetryJobAttempt
import com.zenaton.taskManager.monitoring.global.JobCreated
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalState
import com.zenaton.taskManager.monitoring.perName.JobStatusUpdated
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.taskManager.workers.RunJob
import com.zenaton.taskManager.workers.WorkerMessage

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  Job State
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
     * Admin state
     */
    fun toAvro(obj: MonitoringGlobalState) = convert<AvroMonitoringGlobalState>(obj)
    fun fromAvro(obj: AvroMonitoringGlobalState) = convert<MonitoringGlobalState>(obj)

    /**
     * Admin messages
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
     * Metrics state
     */
    fun toAvro(obj: MonitoringPerNameState) = convert<AvroMonitoringPerNameState>(obj)
    fun fromAvro(obj: AvroMonitoringPerNameState) = convert<MonitoringPerNameState>(obj)

    /**
     * Metrics messages
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
            is JobAttemptDispatched -> builder.apply {
                jobAttemptDispatched = convert<AvroJobAttemptDispatched>(msg)
                type = AvroEngineMessageType.JobAttemptDispatched
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convert<AvroJobAttemptFailed>(msg)
                type = AvroEngineMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convert<AvroJobAttemptStarted>(msg)
                type = AvroEngineMessageType.JobAttemptStarted
            }
            is JobCanceled -> builder.apply {
                jobCanceled = convert<AvroJobCanceled>(msg)
                type = AvroEngineMessageType.JobCanceled
            }
            is JobCompleted -> builder.apply {
                jobCompleted = convert<AvroJobCompleted>(msg)
                type = AvroEngineMessageType.JobCompleted
            }
            is JobDispatched -> builder.apply {
                jobDispatched = convert<AvroJobDispatched>(msg)
                type = AvroEngineMessageType.JobDispatched
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
            AvroEngineMessageType.JobAttemptDispatched -> convert<JobAttemptDispatched>(input.jobAttemptDispatched)
            AvroEngineMessageType.JobAttemptFailed -> convert<JobAttemptFailed>(input.jobAttemptFailed)
            AvroEngineMessageType.JobAttemptStarted -> convert<JobAttemptStarted>(input.jobAttemptStarted)
            AvroEngineMessageType.JobCanceled -> convert<JobCanceled>(input.jobCanceled)
            AvroEngineMessageType.JobCompleted -> convert<JobCompleted>(input.jobCompleted)
            AvroEngineMessageType.JobDispatched -> convert<JobDispatched>(input.jobDispatched)
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse(Json.stringify(from))
}
