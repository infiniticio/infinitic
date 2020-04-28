package com.zenaton.pulsar.topics.delays.messages

import com.zenaton.engine.topics.delays.DelayMessage

interface PulsarDelayMessageConverterInterface {
    fun fromPulsar(input: PulsarDelayMessage): DelayMessage
    fun toPulsar(msg: DelayMessage): PulsarDelayMessage
}
