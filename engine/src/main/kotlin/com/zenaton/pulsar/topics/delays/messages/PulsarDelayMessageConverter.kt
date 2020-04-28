package com.zenaton.pulsar.topics.delays.messages

import com.zenaton.engine.topics.delays.DelayMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface

object PulsarDelayMessageConverter : PulsarDelayMessageConverterInterface {
    var json: JsonInterface = Json

    override fun fromPulsar(input: PulsarDelayMessage): DelayMessage {
        return json.parse(
            json.stringify(input), DelayMessage::class) as DelayMessage
    }

    override fun toPulsar(msg: DelayMessage): PulsarDelayMessage {
        return json.parse(
            json.stringify(msg), PulsarDelayMessage::class) as PulsarDelayMessage
    }
}
