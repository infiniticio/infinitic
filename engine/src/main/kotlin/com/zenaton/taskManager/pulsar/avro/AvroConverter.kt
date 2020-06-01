package com.zenaton.taskManager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskManager.admin.messages.AvroMonitoringGlobalMessage
import com.zenaton.taskManager.admin.messages.AvroMonitoringGlobalMessageType
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.taskManager.monitoring.global.TaskCreated
import com.zenaton.taskManager.monitoring.global.MonitoringGlobalState
import com.zenaton.taskManager.engine.messages.AvroCancelTask
import com.zenaton.taskManager.engine.messages.AvroDispatchTask
import com.zenaton.taskManager.engine.messages.AvroRetryTask
import com.zenaton.taskManager.engine.messages.AvroRetryTaskAttempt
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptCompleted
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptDispatched
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptFailed
import com.zenaton.taskManager.engine.messages.AvroTaskAttemptStarted
import com.zenaton.taskManager.engine.messages.AvroTaskCanceled
import com.zenaton.taskManager.engine.messages.AvroTaskCompleted
import com.zenaton.taskManager.engine.messages.AvroTaskDispatched
import com.zenaton.taskManager.engine.messages.AvroTaskEngineMessage
import com.zenaton.taskManager.engine.messages.AvroTaskEngineMessageType
import com.zenaton.taskManager.engine.CancelTask
import com.zenaton.taskManager.engine.DispatchTask
import com.zenaton.taskManager.engine.RetryTask
import com.zenaton.taskManager.engine.RetryTaskAttempt
import com.zenaton.taskManager.engine.TaskAttemptCompleted
import com.zenaton.taskManager.engine.TaskAttemptDispatched
import com.zenaton.taskManager.engine.TaskAttemptFailed
import com.zenaton.taskManager.engine.TaskAttemptStarted
import com.zenaton.taskManager.engine.TaskCanceled
import com.zenaton.taskManager.engine.TaskCompleted
import com.zenaton.taskManager.engine.TaskDispatched
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.engine.EngineState
import com.zenaton.taskManager.metrics.messages.AvroMonitoringPerNameMessage
import com.zenaton.taskManager.metrics.messages.AvroMonitoringPerNameMessageType
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.monitoring.perName.TaskStatusUpdated
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameState
import com.zenaton.taskManager.states.AvroTaskAdminState
import com.zenaton.taskManager.states.AvroTaskMetricsState
import com.zenaton.taskManager.states.AvroTaskState
import com.zenaton.taskManager.workers.AvroRunTask
import com.zenaton.taskManager.workers.AvroTaskWorkerMessage
import com.zenaton.taskManager.workers.AvroTaskWorkerMessageType
import com.zenaton.taskManager.workers.RunTask
import com.zenaton.taskManager.workers.WorkerMessage

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  Task State
     */
    fun toAvro(obj: EngineState) = convert<AvroTaskState>(obj)
    fun fromAvro(obj: AvroTaskState) = convert<EngineState>(obj)

    /**
     *  Worker messages
     */
    fun toAvro(msg: WorkerMessage): AvroTaskWorkerMessage {
        val builder = AvroTaskWorkerMessage.newBuilder()
        when (msg) {
            is RunTask -> builder.apply {
                runTask = convert<AvroRunTask>(msg)
                type = AvroTaskWorkerMessageType.RunTask
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskWorkerMessage): WorkerMessage {
        return when (val type = input.getType()) {
            AvroTaskWorkerMessageType.RunTask -> convert<RunTask>(input.runTask)
        }
    }

    /**
     * Admin state
     */
    fun toAvro(obj: MonitoringGlobalState) = convert<AvroTaskAdminState>(obj)
    fun fromAvro(obj: AvroTaskAdminState) = convert<MonitoringGlobalState>(obj)

    /**
     * Admin messages
     */
    fun toAvro(msg: MonitoringGlobalMessage): AvroMonitoringGlobalMessage {
        val builder = AvroMonitoringGlobalMessage.newBuilder()
        when (msg) {
            is TaskCreated -> builder.apply {
                taskCreated = convert(msg)
                type = AvroMonitoringGlobalMessageType.TaskCreated
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringGlobalMessage): MonitoringGlobalMessage {
        return when (val type = input.getType()) {
            AvroMonitoringGlobalMessageType.TaskCreated -> convert<TaskCreated>(input.taskCreated)
        }
    }

    /**
     * Metrics state
     */
    fun toAvro(obj: MonitoringPerNameState) = convert<AvroTaskMetricsState>(obj)
    fun fromAvro(obj: AvroTaskMetricsState) = convert<MonitoringPerNameState>(obj)

    /**
     * Metrics messages
     */
    fun toAvro(msg: MonitoringPerNameMessage): AvroMonitoringPerNameMessage {
        val builder = AvroMonitoringPerNameMessage.newBuilder()
        when (msg) {
            is TaskStatusUpdated -> builder.apply {
                taskStatusUpdated = convert(msg)
                type = AvroMonitoringPerNameMessageType.TaskStatusUpdated
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroMonitoringPerNameMessage): MonitoringPerNameMessage {
        return when (val type = input.getType()) {
            AvroMonitoringPerNameMessageType.TaskStatusUpdated -> convert<TaskStatusUpdated>(input.taskStatusUpdated)
        }
    }

    /**
     * Engine messages
     */
    fun toAvro(msg: EngineMessage): AvroTaskEngineMessage {
        val builder = AvroTaskEngineMessage.newBuilder()
        builder.taskId = msg.taskId.id
        when (msg) {
            is CancelTask            -> builder.apply {
                cancelTask = convert<AvroCancelTask>(msg)
                type = AvroTaskEngineMessageType.CancelTask
            }
            is DispatchTask          -> builder.apply {
                dispatchTask = convert<AvroDispatchTask>(msg)
                type = AvroTaskEngineMessageType.DispatchTask
            }
            is RetryTask             -> builder.apply {
                retryTask = convert<AvroRetryTask>(msg)
                type = AvroTaskEngineMessageType.RetryTask
            }
            is RetryTaskAttempt      -> builder.apply {
                retryTaskAttempt = convert<AvroRetryTaskAttempt>(msg)
                type = AvroTaskEngineMessageType.RetryTaskAttempt
            }
            is TaskAttemptCompleted  -> builder.apply {
                taskAttemptCompleted = convert<AvroTaskAttemptCompleted>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptCompleted
            }
            is TaskAttemptDispatched -> builder.apply {
                taskAttemptDispatched = convert<AvroTaskAttemptDispatched>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptDispatched
            }
            is TaskAttemptFailed     -> builder.apply {
                taskAttemptFailed = convert<AvroTaskAttemptFailed>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptFailed
            }
            is TaskAttemptStarted    -> builder.apply {
                taskAttemptStarted = convert<AvroTaskAttemptStarted>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptStarted
            }
            is TaskCanceled          -> builder.apply {
                taskCanceled = convert<AvroTaskCanceled>(msg)
                type = AvroTaskEngineMessageType.TaskCanceled
            }
            is TaskCompleted         -> builder.apply {
                taskCompleted = convert<AvroTaskCompleted>(msg)
                type = AvroTaskEngineMessageType.TaskCompleted
            }
            is TaskDispatched        -> builder.apply {
                taskDispatched = convert<AvroTaskDispatched>(msg)
                type = AvroTaskEngineMessageType.TaskDispatched
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskEngineMessage): EngineMessage {
        return when (val type = input.getType()) {
            AvroTaskEngineMessageType.CancelTask -> convert<CancelTask>(input.cancelTask)
            AvroTaskEngineMessageType.DispatchTask -> convert<DispatchTask>(input.dispatchTask)
            AvroTaskEngineMessageType.RetryTask -> convert<RetryTask>(input.retryTask)
            AvroTaskEngineMessageType.RetryTaskAttempt -> convert<RetryTaskAttempt>(input.retryTaskAttempt)
            AvroTaskEngineMessageType.TaskAttemptCompleted -> convert<TaskAttemptCompleted>(input.taskAttemptCompleted)
            AvroTaskEngineMessageType.TaskAttemptDispatched -> convert<TaskAttemptDispatched>(input.taskAttemptDispatched)
            AvroTaskEngineMessageType.TaskAttemptFailed -> convert<TaskAttemptFailed>(input.taskAttemptFailed)
            AvroTaskEngineMessageType.TaskAttemptStarted -> convert<TaskAttemptStarted>(input.taskAttemptStarted)
            AvroTaskEngineMessageType.TaskCanceled -> convert<TaskCanceled>(input.taskCanceled)
            AvroTaskEngineMessageType.TaskCompleted -> convert<TaskCompleted>(input.taskCompleted)
            AvroTaskEngineMessageType.TaskDispatched -> convert<TaskDispatched>(input.taskDispatched)
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse(Json.stringify(from))
}
