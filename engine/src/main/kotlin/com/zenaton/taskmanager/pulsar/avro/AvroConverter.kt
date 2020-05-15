package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.messages.AvroRunTask
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.AvroTaskMessageType
import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.commands.AvroCancelTask
import com.zenaton.taskmanager.messages.commands.AvroDispatchTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptFailed
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptStarted
import com.zenaton.taskmanager.messages.events.AvroTaskCanceled
import com.zenaton.taskmanager.messages.events.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptFailed
import com.zenaton.taskmanager.messages.events.TaskAttemptStarted
import com.zenaton.taskmanager.messages.events.TaskCanceled
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface
import com.zenaton.taskmanager.state.TaskState
import com.zenaton.taskmanager.states.AvroTaskState
import kotlin.reflect.KClass

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {
    /**
     *  Task State
     */
    fun toAvro(obj: TaskState) = convert(obj, AvroTaskState::class)
    fun fromAvro(obj: AvroTaskState) = convert(obj, TaskState::class)

    /**
     *  Worker Messages
     */
    fun toAvro(obj: RunTask) = convert(obj, AvroRunTask::class)
    fun fromAvro(obj: AvroRunTask) = convert(obj, RunTask::class)

    /**
     *  Tasks Messages
     */
    fun toAvro(msg: TaskMessageInterface): AvroTaskMessage {
        var builder = AvroTaskMessage.newBuilder()
        builder.taskId = msg.taskId.id
        when (msg) {
            is CancelTask -> {
                builder.cancelTask = switch(msg)
                builder.type = AvroTaskMessageType.CancelTask
            }
            is DispatchTask -> {
                builder.dispatchTask = switch(msg)
                builder.type = AvroTaskMessageType.DispatchTask
            }
            is RetryTask -> {
                builder.retryTask = switch(msg)
                builder.type = AvroTaskMessageType.RetryTask
            }
            is RetryTaskAttempt -> {
                builder.retryTaskAttempt = switch(msg)
                builder.type = AvroTaskMessageType.RetryTaskAttempt
            }
            is TaskAttemptCompleted -> {
                builder.taskAttemptCompleted = switch(msg)
                builder.type = AvroTaskMessageType.TaskAttemptCompleted
            }
            is TaskAttemptDispatched -> {
                builder.taskAttemptDispatched = switch(msg)
                builder.type = AvroTaskMessageType.TaskAttemptDispatched
            }
            is TaskAttemptFailed -> {
                builder.taskAttemptFailed = switch(msg)
                builder.type = AvroTaskMessageType.TaskAttemptFailed
            }
            is TaskAttemptStarted -> {
                builder.taskAttemptStarted = switch(msg)
                builder.type = AvroTaskMessageType.TaskAttemptStarted
            }
            is TaskCanceled -> {
                builder.taskCanceled = switch(msg)
                builder.type = AvroTaskMessageType.TaskCanceled
            }
            else -> throw Exception("Unknown task message class ${msg::class}")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskMessage): TaskMessageInterface {
        val type = input.getType()
        return when (type) {
            AvroTaskMessageType.CancelTask -> switch(input.cancelTask)
            AvroTaskMessageType.DispatchTask -> switch(input.dispatchTask)
            AvroTaskMessageType.RetryTask -> switch(input.retryTask)
            AvroTaskMessageType.RetryTaskAttempt -> switch(input.retryTaskAttempt)
            AvroTaskMessageType.TaskAttemptCompleted -> switch(input.taskAttemptCompleted)
            AvroTaskMessageType.TaskAttemptDispatched -> switch(input.taskAttemptDispatched)
            AvroTaskMessageType.TaskAttemptFailed -> switch(input.taskAttemptFailed)
            AvroTaskMessageType.TaskAttemptStarted -> switch(input.taskAttemptStarted)
            AvroTaskMessageType.TaskCanceled -> switch(input.taskCanceled)
            else -> throw Exception("Unknown avro task message type: $type")
        }
    }

    /**
     *  Switching from and to Avro (Tasks commands)
     */
    private fun switch(obj: CancelTask) = convert(obj, AvroCancelTask::class)
    private fun switch(obj: AvroCancelTask) = convert(obj, CancelTask::class)

    private fun switch(obj: DispatchTask) = convert(obj, AvroDispatchTask::class)
    private fun switch(obj: AvroDispatchTask) = convert(obj, DispatchTask::class)

    private fun switch(obj: RetryTask) = convert(obj, AvroRetryTask::class)
    private fun switch(obj: AvroRetryTask) = convert(obj, RetryTask::class)

    private fun switch(obj: RetryTaskAttempt) = convert(obj, AvroRetryTaskAttempt::class)
    private fun switch(obj: AvroRetryTaskAttempt) = convert(obj, RetryTaskAttempt::class)

    /**
     *  Switching from and to Avro (Tasks events)
     */
    private fun switch(obj: TaskAttemptCompleted) = convert(obj, AvroTaskAttemptCompleted::class)
    private fun switch(obj: AvroTaskAttemptCompleted) = convert(obj, TaskAttemptCompleted::class)

    private fun switch(obj: TaskAttemptDispatched) = convert(obj, AvroTaskAttemptDispatched::class)
    private fun switch(obj: AvroTaskAttemptDispatched) = convert(obj, TaskAttemptDispatched::class)

    private fun switch(obj: TaskAttemptFailed) = convert(obj, AvroTaskAttemptFailed::class)
    private fun switch(obj: AvroTaskAttemptFailed) = convert(obj, TaskAttemptFailed::class)

    private fun switch(obj: TaskAttemptStarted) = convert(obj, AvroTaskAttemptStarted::class)
    private fun switch(obj: AvroTaskAttemptStarted) = convert(obj, TaskAttemptStarted::class)

    private fun switch(obj: TaskCanceled) = convert(obj, AvroTaskCanceled::class)
    private fun switch(obj: AvroTaskCanceled) = convert(obj, TaskCanceled::class)

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private fun <T : Any> convert(from: Any, to: KClass<T>): T = Json.parse(Json.stringify(from), to)
}
