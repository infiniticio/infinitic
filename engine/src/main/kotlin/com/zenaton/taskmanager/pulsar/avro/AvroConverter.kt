package com.zenaton.taskmanager.pulsar.avro

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.messages.AvroTaskAttemptCompleted
import com.zenaton.taskmanager.messages.AvroTaskAttemptFailed
import com.zenaton.taskmanager.messages.AvroTaskAttemptMessage
import com.zenaton.taskmanager.messages.AvroTaskAttemptRetried
import com.zenaton.taskmanager.messages.AvroTaskAttemptStarted
import com.zenaton.taskmanager.messages.AvroTaskAttemptTimeout
import com.zenaton.taskmanager.messages.AvroTaskDispatched
import com.zenaton.taskmanager.messages.AvroTaskMessage
import com.zenaton.taskmanager.messages.AvroTaskMessageType
import com.zenaton.taskmanager.messages.TaskAttemptCompleted
import com.zenaton.taskmanager.messages.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.TaskAttemptFailed
import com.zenaton.taskmanager.messages.TaskAttemptRetried
import com.zenaton.taskmanager.messages.TaskAttemptStarted
import com.zenaton.taskmanager.messages.TaskAttemptTimeout
import com.zenaton.taskmanager.messages.TaskDispatched
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
     *  Task Attempts Messages
     */
    fun toAvro(obj: TaskAttemptDispatched) = convert(obj, AvroTaskAttemptMessage::class)
    fun fromAvro(obj: AvroTaskAttemptMessage) = convert(obj, TaskAttemptDispatched::class)

    /**
     *  Tasks Messages
     */
    fun toAvro(obj: TaskAttemptCompleted) = convert(obj, AvroTaskAttemptCompleted::class)
    fun fromAvro(obj: AvroTaskAttemptCompleted) = convert(obj, TaskAttemptCompleted::class)

    fun toAvro(obj: TaskAttemptFailed) = convert(obj, AvroTaskAttemptFailed::class)
    fun fromAvro(obj: AvroTaskAttemptFailed) = convert(obj, TaskAttemptFailed::class)

    fun toAvro(obj: TaskAttemptRetried) = convert(obj, AvroTaskAttemptRetried::class)
    fun fromAvro(obj: AvroTaskAttemptRetried) = convert(obj, TaskAttemptRetried::class)

    fun toAvro(obj: TaskAttemptStarted) = convert(obj, AvroTaskAttemptStarted::class)
    fun fromAvro(obj: AvroTaskAttemptStarted) = convert(obj, TaskAttemptStarted::class)

    fun toAvro(obj: TaskAttemptTimeout) = convert(obj, AvroTaskAttemptTimeout::class)
    fun fromAvro(obj: AvroTaskAttemptTimeout) = convert(obj, TaskAttemptTimeout::class)

    fun toAvro(obj: TaskDispatched) = convert(obj, AvroTaskDispatched::class)
    fun fromAvro(obj: AvroTaskDispatched) = convert(obj, TaskDispatched::class)

    fun toAvro(msg: TaskMessageInterface): AvroTaskMessage {
        var builder = AvroTaskMessage.newBuilder()
        builder.taskId = msg.taskId.id
        builder = when (msg) {
            is TaskAttemptCompleted -> builder
                .setTaskAttemptCompleted(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptCompleted)
            is TaskAttemptFailed -> builder
                .setTaskAttemptFailed(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptFailed)
            is TaskAttemptRetried -> builder
                .setTaskAttemptRetried(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptRetried)
            is TaskAttemptStarted -> builder
                .setTaskAttemptStarted(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptStarted)
            is TaskAttemptTimeout -> builder
                .setTaskAttemptTimeout(toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptTimeout)
            is TaskDispatched -> builder
                .setTaskDispatched(toAvro(msg))
                .setType(AvroTaskMessageType.TaskDispatched)
            else -> throw Exception("Unknown task message class ${msg::class}")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskMessage): TaskMessageInterface {
        val type = input.getType()
        return when (type) {
            AvroTaskMessageType.TaskAttemptCompleted -> fromAvro(input.getTaskAttemptCompleted())
            AvroTaskMessageType.TaskAttemptFailed -> fromAvro(input.getTaskAttemptFailed())
            AvroTaskMessageType.TaskAttemptRetried -> fromAvro(input.getTaskAttemptRetried())
            AvroTaskMessageType.TaskAttemptStarted -> fromAvro(input.getTaskAttemptStarted())
            AvroTaskMessageType.TaskAttemptTimeout -> fromAvro(input.getTaskAttemptTimeout())
            AvroTaskMessageType.TaskDispatched -> fromAvro(input.getTaskDispatched())
            else -> throw Exception("Unknown avro task message type: $type")
        }
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private fun <T : Any> convert(from: Any, to: KClass<T>): T = Json.parse(Json.stringify(from), to)
}
