package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.data.TaskState
import com.zenaton.taskmanager.messages.engine.AvroCancelTask
import com.zenaton.taskmanager.messages.engine.AvroDispatchTask
import com.zenaton.taskmanager.messages.engine.AvroRetryTask
import com.zenaton.taskmanager.messages.engine.AvroRetryTaskAttempt
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptDispatched
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptFailed
import com.zenaton.taskmanager.messages.engine.AvroTaskAttemptStarted
import com.zenaton.taskmanager.messages.engine.AvroTaskCanceled
import com.zenaton.taskmanager.messages.engine.AvroTaskCompleted
import com.zenaton.taskmanager.messages.engine.AvroTaskDispatched
import com.zenaton.taskmanager.messages.engine.AvroTaskEngineMessage
import com.zenaton.taskmanager.messages.engine.AvroTaskEngineMessageType
import com.zenaton.taskmanager.messages.engine.CancelTask
import com.zenaton.taskmanager.messages.engine.DispatchTask
import com.zenaton.taskmanager.messages.engine.RetryTask
import com.zenaton.taskmanager.messages.engine.RetryTaskAttempt
import com.zenaton.taskmanager.messages.engine.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.engine.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.engine.TaskAttemptFailed
import com.zenaton.taskmanager.messages.engine.TaskAttemptStarted
import com.zenaton.taskmanager.messages.engine.TaskCanceled
import com.zenaton.taskmanager.messages.engine.TaskCompleted
import com.zenaton.taskmanager.messages.engine.TaskDispatched
import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.metrics.AvroTaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.AvroTaskMetricMessageType
import com.zenaton.taskmanager.messages.metrics.AvroTaskStatusUpdated
import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.messages.workers.AvroRunTask
import com.zenaton.taskmanager.messages.workers.AvroTaskWorkerMessage
import com.zenaton.taskmanager.messages.workers.AvroTaskWorkerMessageType
import com.zenaton.taskmanager.messages.workers.RunTask
import com.zenaton.taskmanager.messages.workers.TaskWorkerMessage
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.states.AvroTaskMetricsState
import com.zenaton.taskmanager.states.AvroTaskState

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object TaskAvroConverter {
    /**
     *  Task State
     */
    fun toAvro(obj: TaskState) = convert<AvroTaskState>(obj)
    fun fromAvro(obj: AvroTaskState) = convert<TaskState>(obj)

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
                taskStatusUpdated = convert<AvroTaskStatusUpdated>(msg)
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
