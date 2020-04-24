package com.zenaton.pulsar.workflows.serializers

import com.zenaton.engine.topics.workflows.WorkflowMessage
import com.zenaton.pulsar.workflows.PulsarMessage

interface MessageConverterInterface {
    fun fromPulsar(input: PulsarMessage): WorkflowMessage
    fun toPulsar(msg: WorkflowMessage): PulsarMessage
}
