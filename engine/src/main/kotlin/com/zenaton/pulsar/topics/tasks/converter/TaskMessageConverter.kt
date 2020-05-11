package com.zenaton.pulsar.topics.tasks.converter

import com.zenaton.engine.topics.tasks.interfaces.TaskMessageInterface
import com.zenaton.engine.topics.tasks.messages.TaskAttemptCompleted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptFailed
import com.zenaton.engine.topics.tasks.messages.TaskAttemptRetried
import com.zenaton.engine.topics.tasks.messages.TaskAttemptStarted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptTimeout
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.messages.tasks.AvroTaskMessage
import com.zenaton.messages.tasks.AvroTaskMessageType
import com.zenaton.pulsar.utils.AvroConverter

/**
 * This object provides methods to convert a TaskMessageInterface into an AvroTaskMessage, and back
 */
object TaskMessageConverter {
    fun toAvro(msg: TaskMessageInterface): AvroTaskMessage {
        var builder = AvroTaskMessage.newBuilder()
        builder = when (msg) {
            is TaskAttemptCompleted -> builder
                .setTaskAttemptCompleted(AvroConverter.toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptCompleted)
            is TaskAttemptFailed -> builder
                .setTaskAttemptFailed(AvroConverter.toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptFailed)
            is TaskAttemptRetried -> builder
                .setTaskAttemptRetried(AvroConverter.toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptRetried)
            is TaskAttemptStarted -> builder
                .setTaskAttemptStarted(AvroConverter.toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptStarted)
            is TaskAttemptTimeout -> builder
                .setTaskAttemptTimeout(AvroConverter.toAvro(msg))
                .setType(AvroTaskMessageType.TaskAttemptTimeout)
            is TaskDispatched -> builder
                .setTaskDispatched(AvroConverter.toAvro(msg))
                .setType(AvroTaskMessageType.TaskDispatched)
            else -> throw Exception("Unknown task message class ${msg::class}")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskMessage): TaskMessageInterface {
        val type = input.getType()
        return when (type) {
            AvroTaskMessageType.TaskAttemptCompleted -> AvroConverter.fromAvro(input.getTaskAttemptCompleted())
            AvroTaskMessageType.TaskAttemptFailed -> AvroConverter.fromAvro(input.getTaskAttemptFailed())
            AvroTaskMessageType.TaskAttemptRetried -> AvroConverter.fromAvro(input.getTaskAttemptRetried())
            AvroTaskMessageType.TaskAttemptStarted -> AvroConverter.fromAvro(input.getTaskAttemptStarted())
            AvroTaskMessageType.TaskAttemptTimeout -> AvroConverter.fromAvro(input.getTaskAttemptTimeout())
            AvroTaskMessageType.TaskDispatched -> AvroConverter.fromAvro(input.getTaskDispatched())
            else -> throw Exception("Unknown avro task message type: $type")
        }
    }
}
