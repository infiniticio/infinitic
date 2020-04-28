package com.zenaton.pulsar.topics.tasks.messages

import com.zenaton.engine.topics.tasks.TaskMessage

interface PulsarTaskMessageConverterInterface {
    fun fromPulsar(input: PulsarTaskMessage): TaskMessage
    fun toPulsar(msg: TaskMessage): PulsarTaskMessage
}
