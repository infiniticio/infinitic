package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.messages.AvroRunTask
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.AvroTaskMessageType
import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.commands.AvroDispatchTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTask
import com.zenaton.taskmanager.messages.commands.AvroRetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.AvroTimeOutTaskAttempt
import com.zenaton.taskmanager.messages.commands.DispatchTask
import com.zenaton.taskmanager.messages.commands.RetryTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.TimeOutTaskAttempt
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptFailed
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptStarted
import com.zenaton.taskmanager.messages.events.AvroTaskAttemptTimedOut
import com.zenaton.taskmanager.messages.events.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptFailed
import com.zenaton.taskmanager.messages.events.TaskAttemptStarted
import com.zenaton.taskmanager.messages.events.TaskAttemptTimedOut
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
     *  Worker Task Messages
     */
    fun toAvro(obj: RunTask) = convert(obj, AvroRunTask::class)
    fun fromAvro(obj: AvroRunTask) = convert(obj, RunTask::class)

    /**
     *  Commands Tasks Messages
     */

    fun toAvro(obj: DispatchTask) = convert(obj, AvroDispatchTask::class)
    fun fromAvro(obj: AvroDispatchTask) = convert(obj, DispatchTask::class)

    fun toAvro(obj: RetryTask) = convert(obj, AvroRetryTask::class)
    fun fromAvro(obj: AvroRetryTask) = convert(obj, RetryTask::class)

    fun toAvro(obj: RetryTaskAttempt) = convert(obj, AvroRetryTaskAttempt::class)
    fun fromAvro(obj: AvroRetryTaskAttempt) = convert(obj, RetryTaskAttempt::class)

    fun toAvro(obj: TimeOutTaskAttempt) = convert(obj, AvroTimeOutTaskAttempt::class)
    fun fromAvro(obj: AvroTimeOutTaskAttempt) = convert(obj, TimeOutTaskAttempt::class)

    /**
     *  Events Tasks Messages
     */
    fun toAvro(obj: TaskAttemptCompleted) = convert(obj, AvroTaskAttemptCompleted::class)
    fun fromAvro(obj: AvroTaskAttemptCompleted) = convert(obj, TaskAttemptCompleted::class)

    fun toAvro(obj: TaskAttemptDispatched) = convert(obj, AvroTaskAttemptDispatched::class)
    fun fromAvro(obj: AvroTaskAttemptDispatched) = convert(obj, TaskAttemptDispatched::class)

    fun toAvro(obj: TaskAttemptFailed) = convert(obj, AvroTaskAttemptFailed::class)
    fun fromAvro(obj: AvroTaskAttemptFailed) = convert(obj, TaskAttemptFailed::class)

    fun toAvro(obj: TaskAttemptStarted) = convert(obj, AvroTaskAttemptStarted::class)
    fun fromAvro(obj: AvroTaskAttemptStarted) = convert(obj, TaskAttemptStarted::class)

    fun toAvro(obj: TaskAttemptTimedOut) = convert(obj, AvroTaskAttemptTimedOut::class)
    fun fromAvro(obj: AvroTaskAttemptTimedOut) = convert(obj, TaskAttemptTimedOut::class)

    fun toAvro(msg: TaskMessageInterface): AvroTaskMessage {
        var builder = AvroTaskMessage.newBuilder()
        builder.taskId = msg.taskId.id
        builder = when (msg) {
            is DispatchTask -> builder
                .setDispatchTask(toAvro(msg))
                .setType(AvroTaskMessageType.DispatchTask)
            is RetryTask -> builder
                .setRetryTask(toAvro(msg))
                .setType(AvroTaskMessageType.RetryTask)
            is RetryTaskAttempt -> builder
                .setRetryTaskAttempt(toAvro(msg))
                .setType(AvroTaskMessageType.RetryTaskAttempt)
            is TimeOutTaskAttempt -> builder
                .setTimeOutTaskAttempt(toAvro(msg))
                .setType(AvroTaskMessageType.TimeoutTaskAttempt)
            is TaskAttemptCompleted -> builder
                .setTaskAttemptCompleted(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptCompleted)
            is TaskAttemptDispatched -> builder
                .setTaskAttemptDispatched(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptDispatched)
            is TaskAttemptFailed -> builder
                .setTaskAttemptFailed(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptFailed)
            is TaskAttemptStarted -> builder
                .setTaskAttemptStarted(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptStarted)
            is TaskAttemptTimedOut -> builder
                .setTaskAttemptTimedOut(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptTimedOut)
            else -> throw Exception("Unknown task message class ${msg::class}")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskMessage): TaskMessageInterface {
        val type = input.getType()
        return when (type) {
            AvroTaskMessageType.DispatchTask -> fromAvro(input.getDispatchTask())
            AvroTaskMessageType.RetryTask -> fromAvro(input.getRetryTask())
            AvroTaskMessageType.RetryTaskAttempt -> fromAvro(input.getRetryTaskAttempt())
            AvroTaskMessageType.TimeoutTaskAttempt -> fromAvro(input.getTimeOutTaskAttempt())
            AvroTaskMessageType.TaskAttemptCompleted -> fromAvro(input.getTaskAttemptCompleted())
            AvroTaskMessageType.TaskAttemptFailed -> fromAvro(input.getTaskAttemptFailed())
            AvroTaskMessageType.TaskAttemptStarted -> fromAvro(input.getTaskAttemptStarted())
            AvroTaskMessageType.TaskAttemptTimedOut -> fromAvro(input.getTaskAttemptTimedOut())
            else -> throw Exception("Unknown avro task message type: $type")
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private fun <T : Any> convert(from: Any, to: KClass<T>): T = Json.parse(Json.stringify(from), to)
}
