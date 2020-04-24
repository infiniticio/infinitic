package com.zenaton.pulsar.workflows.serializers

import com.zenaton.engine.topics.workflows.WorkflowMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface
import com.zenaton.pulsar.workflows.PulsarMessage

object MessageConverter : MessageConverterInterface {
    var json: JsonInterface = Json

    override fun fromPulsar(input: PulsarMessage): WorkflowMessage {
        return json.parse(json.stringify(input), WorkflowMessage::class) as WorkflowMessage
    }

    override fun toPulsar(msg: WorkflowMessage): PulsarMessage {
        return json.parse(json.stringify(msg), PulsarMessage::class) as PulsarMessage
    }
}
