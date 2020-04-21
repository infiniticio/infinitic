package com.zenaton.pulsar.workflows.serializers

import com.zenaton.engine.workflows.WorkflowMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface
import com.zenaton.pulsar.workflows.PulsarMessage

object MessageConverter : MessageConverterInterface {
    var json: JsonInterface = Json

    override fun fromPulsar(input: PulsarMessage): WorkflowMessage {
        return json.from(json.to(input), WorkflowMessage::class) as WorkflowMessage
    }

    override fun toPulsar(msg: WorkflowMessage): PulsarMessage {
        return json.from(json.to(msg), PulsarMessage::class) as PulsarMessage
    }
}
