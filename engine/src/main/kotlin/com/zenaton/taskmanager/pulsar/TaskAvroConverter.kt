package com.zenaton.taskmanager.pulsar

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.messages.AvroRunTask
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.AvroTaskMessageType
import com.zenaton.taskmanager.messages.TaskMessageInterface
import com.zenaton.taskmanager.messages.commands.AvroCancelTask
import com.zenaton.taskmanager.messages.commands.AvroDispatchTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.RunTask
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
import com.zenaton.taskmanager.state.TaskState
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
     *  Worker Messages
     */
    fun toAvro(obj: RunTask) = convert<AvroRunTask>(obj)
    fun fromAvro(obj: AvroRunTask) = convert<RunTask>(obj)

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

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convert(from: Any): T = Json.parse<T>(Json.stringify(from))
}
