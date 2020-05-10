package com.zenaton.pulsar.topics.taskAttempts.converter

import com.zenaton.engine.topics.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.messages.topics.taskAttempts.AvroTaskAttemptDispatched
import com.zenaton.utils.json.Json

object TaskAttemptConverter {
    fun toAvro(msg: TaskAttemptDispatched) = Json.parse(Json.stringify(msg), AvroTaskAttemptDispatched::class)

    fun fromAvro(input: AvroTaskAttemptDispatched) = Json.parse(Json.stringify(input), TaskAttemptDispatched::class)
}
