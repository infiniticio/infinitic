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
import com.zenaton.taskmanager.messages.metrics.AvroTaskStatusUpdated
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.messages.workers.AvroRunTask
import com.zenaton.taskmanager.messages.workers.RunTask
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
     *  Worker message
     */
    fun toAvro(obj: RunTask) = convert<AvroRunTask>(obj)

    /**
     * Metrics message
     */
    fun toAvro(obj: TaskStatusUpdated) = convert<AvroTaskStatusUpdated>(obj)

    /**
     * Engine messages
     */
    fun toAvro(msg: TaskEngineMessage): AvroTaskEngineMessage {
        val builder = AvroTaskEngineMessage.newBuilder()
        builder.taskId = msg.taskId.id
        when (msg) {
            is CancelTask -> builder.apply {
                cancelTask = switch(msg)
                type = AvroTaskEngineMessageType.CancelTask
            }
            is DispatchTask -> builder.apply {
                dispatchTask = switch(msg)
                type = AvroTaskEngineMessageType.DispatchTask
            }
            is RetryTask -> builder.apply {
                retryTask = switch(msg)
                type = AvroTaskEngineMessageType.RetryTask
            }
            is RetryTaskAttempt -> builder.apply {
                retryTaskAttempt = switch(msg)
                type = AvroTaskEngineMessageType.RetryTaskAttempt
            }
            is TaskAttemptCompleted -> builder.apply {
                taskAttemptCompleted = switch(msg)
                type = AvroTaskEngineMessageType.TaskAttemptCompleted
            }
            is TaskAttemptDispatched -> builder.apply {
                taskAttemptDispatched = switch(msg)
                type = AvroTaskEngineMessageType.TaskAttemptDispatched
            }
            is TaskAttemptFailed -> builder.apply {
                taskAttemptFailed = switch(msg)
                type = AvroTaskEngineMessageType.TaskAttemptFailed
            }
            is TaskAttemptStarted -> builder.apply {
                taskAttemptStarted = switch(msg)
                type = AvroTaskEngineMessageType.TaskAttemptStarted
            }
            is TaskCanceled -> builder.apply {
                taskCanceled = switch(msg)
                type = AvroTaskEngineMessageType.TaskCanceled
            }
            is TaskCompleted -> builder.apply {
                taskCompleted = switch(msg)
                type = AvroTaskEngineMessageType.TaskCompleted
            }
            is TaskDispatched -> builder.apply {
                taskDispatched = switch(msg)
                type = AvroTaskEngineMessageType.TaskDispatched
            }
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskEngineMessage): TaskEngineMessage {
        return when (val type = input.getType()) {
            AvroTaskEngineMessageType.CancelTask -> switch(input.cancelTask)
            AvroTaskEngineMessageType.DispatchTask -> switch(input.dispatchTask)
            AvroTaskEngineMessageType.RetryTask -> switch(input.retryTask)
            AvroTaskEngineMessageType.RetryTaskAttempt -> switch(input.retryTaskAttempt)
            AvroTaskEngineMessageType.TaskAttemptCompleted -> switch(input.taskAttemptCompleted)
            AvroTaskEngineMessageType.TaskAttemptDispatched -> switch(input.taskAttemptDispatched)
            AvroTaskEngineMessageType.TaskAttemptFailed -> switch(input.taskAttemptFailed)
            AvroTaskEngineMessageType.TaskAttemptStarted -> switch(input.taskAttemptStarted)
            AvroTaskEngineMessageType.TaskCanceled -> switch(input.taskCanceled)
            AvroTaskEngineMessageType.TaskCompleted -> switch(input.taskCompleted)
            AvroTaskEngineMessageType.TaskDispatched -> switch(input.taskDispatched)
        }
    }

    /**
     *  Switching from and to Avro (Tasks commands)
     */
    private fun switch(obj: CancelTask) = convert<AvroCancelTask>(obj)
    private fun switch(obj: AvroCancelTask) = convert<CancelTask>(obj)

    private fun switch(obj: DispatchTask) = convert<AvroDispatchTask>(obj)
    private fun switch(obj: AvroDispatchTask) = convert<DispatchTask>(obj)

    private fun switch(obj: RetryTask) = convert<AvroRetryTask>(obj)
    private fun switch(obj: AvroRetryTask) = convert<RetryTask>(obj)

    private fun switch(obj: RetryTaskAttempt) = convert<AvroRetryTaskAttempt>(obj)
    private fun switch(obj: AvroRetryTaskAttempt) = convert<RetryTaskAttempt>(obj)

    /**
     *  Switching from and to Avro (Tasks events)
     */
    private fun switch(obj: TaskAttemptCompleted) = convert<AvroTaskAttemptCompleted>(obj)
    private fun switch(obj: AvroTaskAttemptCompleted) = convert<TaskAttemptCompleted>(obj)

    private fun switch(obj: TaskAttemptDispatched) = convert<AvroTaskAttemptDispatched>(obj)
    private fun switch(obj: AvroTaskAttemptDispatched) = convert<TaskAttemptDispatched>(obj)

    private fun switch(obj: TaskAttemptFailed) = convert<AvroTaskAttemptFailed>(obj)
    private fun switch(obj: AvroTaskAttemptFailed) = convert<TaskAttemptFailed>(obj)

    private fun switch(obj: TaskAttemptStarted) = convert<AvroTaskAttemptStarted>(obj)
    private fun switch(obj: AvroTaskAttemptStarted) = convert<TaskAttemptStarted>(obj)

    private fun switch(obj: TaskCanceled) = convert<AvroTaskCanceled>(obj)
    private fun switch(obj: AvroTaskCanceled) = convert<TaskCanceled>(obj)

    private fun switch(obj: TaskCompleted) = convert<AvroTaskCompleted>(obj)
    private fun switch(obj: AvroTaskCompleted) = convert<TaskCompleted>(obj)

    private fun switch(obj: TaskDispatched) = convert<AvroTaskDispatched>(obj)
    private fun switch(obj: AvroTaskDispatched) = convert<TaskDispatched>(obj)

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse(Json.stringify(from))
}
