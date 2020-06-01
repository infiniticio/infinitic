package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.admin.messages.AvroTaskAdminMessage
import com.zenaton.taskmanager.admin.messages.AvroTaskAdminMessageType
import com.zenaton.taskmanager.admin.messages.TaskAdminMessage
import com.zenaton.taskmanager.admin.messages.TaskTypeCreated
import com.zenaton.taskmanager.admin.state.TaskAdminState
import com.zenaton.taskmanager.engine.messages.AvroCancelTask
import com.zenaton.taskmanager.engine.messages.AvroDispatchTask
import com.zenaton.taskmanager.engine.messages.AvroRetryTask
import com.zenaton.taskmanager.engine.messages.AvroRetryTaskAttempt
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptDispatched
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptFailed
import com.zenaton.taskmanager.engine.messages.AvroTaskAttemptStarted
import com.zenaton.taskmanager.engine.messages.AvroTaskCanceled
import com.zenaton.taskmanager.engine.messages.AvroTaskCompleted
import com.zenaton.taskmanager.engine.messages.AvroTaskDispatched
import com.zenaton.taskmanager.engine.messages.AvroTaskEngineMessage
import com.zenaton.taskmanager.engine.messages.AvroTaskEngineMessageType
import com.zenaton.taskmanager.engine.messages.CancelTask
import com.zenaton.taskmanager.engine.messages.DispatchTask
import com.zenaton.taskmanager.engine.messages.RetryTask
import com.zenaton.taskmanager.engine.messages.RetryTaskAttempt
import com.zenaton.taskmanager.engine.messages.TaskAttemptCompleted
import com.zenaton.taskmanager.engine.messages.TaskAttemptDispatched
import com.zenaton.taskmanager.engine.messages.TaskAttemptFailed
import com.zenaton.taskmanager.engine.messages.TaskAttemptStarted
import com.zenaton.taskmanager.engine.messages.TaskCanceled
import com.zenaton.taskmanager.engine.messages.TaskCompleted
import com.zenaton.taskmanager.engine.messages.TaskDispatched
import com.zenaton.taskmanager.engine.messages.TaskEngineMessage
import com.zenaton.taskmanager.engine.state.TaskEngineState
import com.zenaton.taskmanager.metrics.messages.AvroTaskMetricMessage
import com.zenaton.taskmanager.metrics.messages.AvroTaskMetricMessageType
import com.zenaton.taskmanager.metrics.messages.TaskMetricMessage
import com.zenaton.taskmanager.metrics.messages.TaskStatusUpdated
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.states.AvroTaskAdminState
import com.zenaton.taskmanager.states.AvroTaskMetricsState
import com.zenaton.taskmanager.states.AvroTaskState
import com.zenaton.taskmanager.workers.AvroRunTask
import com.zenaton.taskmanager.workers.AvroTaskWorkerMessage
import com.zenaton.taskmanager.workers.AvroTaskWorkerMessageType
import com.zenaton.taskmanager.workers.messages.RunTask
import com.zenaton.taskmanager.workers.messages.TaskWorkerMessage

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object TaskAvroConverter {
    /**
     *  Task State
     */
    fun toAvro(obj: TaskEngineState) = convert<AvroTaskState>(obj)
    fun fromAvro(obj: AvroTaskState) = convert<TaskEngineState>(obj)

    /**
     *  Worker messages
     */
    fun toAvro(msg: TaskWorkerMessage): AvroTaskWorkerMessage {
        val builder = AvroTaskWorkerMessage.newBuilder()
        when (msg) {
            is RunTask -> builder.apply {
                runTask = convert<AvroRunTask>(msg)
                type = AvroTaskWorkerMessageType.RunTask
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskWorkerMessage): TaskWorkerMessage {
        return when (val type = input.getType()) {
            AvroTaskWorkerMessageType.RunTask -> convert<RunTask>(input.runTask)
        }
    }

    /**
     * Admin state
     */
    fun toAvro(obj: TaskAdminState) = convert<AvroTaskAdminState>(obj)
    fun fromAvro(obj: AvroTaskAdminState) = convert<TaskAdminState>(obj)

    /**
     * Admin messages
     */
    fun toAvro(msg: TaskAdminMessage): AvroTaskAdminMessage {
        val builder = AvroTaskAdminMessage.newBuilder()
        when (msg) {
            is TaskTypeCreated -> builder.apply {
                taskTypeCreated = convert(msg)
                type = AvroTaskAdminMessageType.TaskTypeCreated
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskAdminMessage): TaskAdminMessage {
        return when (val type = input.getType()) {
            AvroTaskAdminMessageType.TaskTypeCreated -> convert<TaskTypeCreated>(input.taskTypeCreated)
        }
    }

    /**
     * Metrics state
     */
    fun toAvro(obj: TaskMetricsState) = convert<AvroTaskMetricsState>(obj)
    fun fromAvro(obj: AvroTaskMetricsState) = convert<TaskMetricsState>(obj)

    /**
     * Metrics messages
     */
    fun toAvro(msg: TaskMetricMessage): AvroTaskMetricMessage {
        val builder = AvroTaskMetricMessage.newBuilder()
        when (msg) {
            is TaskStatusUpdated -> builder.apply {
                taskStatusUpdated = convert(msg)
                type = AvroTaskMetricMessageType.TaskStatusUpdated
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskMetricMessage): TaskMetricMessage {
        return when (val type = input.getType()) {
            AvroTaskMetricMessageType.TaskStatusUpdated -> convert<TaskStatusUpdated>(input.taskStatusUpdated)
        }
    }

    /**
     * Engine messages
     */
    fun toAvro(msg: TaskEngineMessage): AvroTaskEngineMessage {
        val builder = AvroTaskEngineMessage.newBuilder()
        builder.taskId = msg.taskId.id
        when (msg) {
            is CancelTask -> builder.apply {
                cancelTask = convert<AvroCancelTask>(msg)
                type = AvroTaskEngineMessageType.CancelTask
            }
            is DispatchTask -> builder.apply {
                dispatchTask = convert<AvroDispatchTask>(msg)
                type = AvroTaskEngineMessageType.DispatchTask
            }
            is RetryTask -> builder.apply {
                retryTask = convert<AvroRetryTask>(msg)
                type = AvroTaskEngineMessageType.RetryTask
            }
            is RetryTaskAttempt -> builder.apply {
                retryTaskAttempt = convert<AvroRetryTaskAttempt>(msg)
                type = AvroTaskEngineMessageType.RetryTaskAttempt
            }
            is TaskAttemptCompleted -> builder.apply {
                taskAttemptCompleted = convert<AvroTaskAttemptCompleted>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptCompleted
            }
            is TaskAttemptDispatched -> builder.apply {
                taskAttemptDispatched = convert<AvroTaskAttemptDispatched>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptDispatched
            }
            is TaskAttemptFailed -> builder.apply {
                taskAttemptFailed = convert<AvroTaskAttemptFailed>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptFailed
            }
            is TaskAttemptStarted -> builder.apply {
                taskAttemptStarted = convert<AvroTaskAttemptStarted>(msg)
                type = AvroTaskEngineMessageType.TaskAttemptStarted
            }
            is TaskCanceled -> builder.apply {
                taskCanceled = convert<AvroTaskCanceled>(msg)
                type = AvroTaskEngineMessageType.TaskCanceled
            }
            is TaskCompleted -> builder.apply {
                taskCompleted = convert<AvroTaskCompleted>(msg)
                type = AvroTaskEngineMessageType.TaskCompleted
            }
            is TaskDispatched -> builder.apply {
                taskDispatched = convert<AvroTaskDispatched>(msg)
                type = AvroTaskEngineMessageType.TaskDispatched
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskEngineMessage): TaskEngineMessage {
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
