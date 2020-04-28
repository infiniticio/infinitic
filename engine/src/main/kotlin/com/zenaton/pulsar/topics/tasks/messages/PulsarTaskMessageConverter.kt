package com.zenaton.pulsar.topics.tasks.messages

import com.zenaton.engine.topics.tasks.TaskMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface

object PulsarTaskMessageConverter : PulsarTaskMessageConverterInterface {
    var json: JsonInterface = Json

    override fun fromPulsar(input: PulsarTaskMessage): TaskMessage {
        return json.parse(
            json.stringify(input), TaskMessage::class) as TaskMessage
    }

    override fun toPulsar(msg: TaskMessage): PulsarTaskMessage {
        return json.parse(
            json.stringify(msg), PulsarTaskMessage::class) as PulsarTaskMessage
    }
}
