package com.zenaton.pulsar.topics.tasks.converter

import com.zenaton.engine.topics.tasks.interfaces.TaskMessageInterface
import com.zenaton.engine.topics.tasks.messages.TaskAttemptCompleted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptFailed
import com.zenaton.engine.topics.tasks.messages.TaskAttemptRetried
import com.zenaton.engine.topics.tasks.messages.TaskAttemptStarted
import com.zenaton.engine.topics.tasks.messages.TaskAttemptTimeout
import com.zenaton.engine.topics.tasks.messages.TaskDispatched
import com.zenaton.messages.topics.tasks.AvroTaskAttemptCompleted
import com.zenaton.messages.topics.tasks.AvroTaskAttemptFailed
import com.zenaton.messages.topics.tasks.AvroTaskAttemptRetried
import com.zenaton.messages.topics.tasks.AvroTaskAttemptStarted
import com.zenaton.messages.topics.tasks.AvroTaskAttemptTimeout
import com.zenaton.messages.topics.tasks.AvroTaskDispatched
import com.zenaton.messages.topics.tasks.AvroTaskMessage
import com.zenaton.messages.topics.tasks.MessageType
import com.zenaton.pulsar.utils.Json

object TaskConverter {
    fun toAvro(msg: TaskMessageInterface): AvroTaskMessage {
        var builder = AvroTaskMessage.newBuilder()
        builder = when (msg) {
            is TaskAttemptCompleted -> builder
                .setMsg(Json.parse(Json.stringify(msg), AvroTaskAttemptCompleted::class))
                .setType(MessageType.TaskAttemptCompleted)
            is TaskAttemptFailed -> builder
                .setMsg(Json.parse(Json.stringify(msg), AvroTaskAttemptFailed::class))
                .setType(MessageType.TaskAttemptFailed)
            is TaskAttemptRetried -> builder
                .setMsg(Json.parse(Json.stringify(msg), AvroTaskAttemptRetried::class))
                .setType(MessageType.TaskAttemptRetried)
            is TaskAttemptStarted -> builder
                .setMsg(Json.parse(Json.stringify(msg), AvroTaskAttemptStarted::class))
                .setType(MessageType.TaskAttemptStarted)
            is TaskAttemptTimeout -> builder
                .setMsg(Json.parse(Json.stringify(msg), AvroTaskAttemptTimeout::class))
                .setType(MessageType.TaskAttemptTimeout)
            is TaskDispatched -> builder
                .setMsg(Json.parse(Json.stringify(msg), AvroTaskDispatched::class))
                .setType(MessageType.TaskDispatched)
            else -> throw Exception("Unknown message type ${msg::class}")
        }
        return builder.build()
    }

    fun fromAvro(input: AvroTaskMessage): TaskMessageInterface {
        val o = input.getMsg()
        val klass = when (o) {
            is AvroTaskAttemptCompleted -> TaskAttemptCompleted::class
            is AvroTaskAttemptFailed -> TaskAttemptFailed::class
            is AvroTaskAttemptRetried -> TaskAttemptRetried::class
            is AvroTaskAttemptStarted -> TaskAttemptStarted::class
            is AvroTaskAttemptTimeout -> TaskAttemptTimeout::class
            is AvroTaskDispatched -> TaskDispatched::class
            else -> throw Exception("Unknown message type ${o::class}")
        }
        return Json.parse(o.toString(), klass)
    }
}
