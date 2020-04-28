package com.zenaton.pulsar.topics.workflows.messages

import com.zenaton.engine.topics.workflows.WorkflowMessage
import com.zenaton.pulsar.utils.Json
import com.zenaton.pulsar.utils.JsonInterface

object PulsarWorkflowMessageConverter : PulsarWorkflowMessageConverterInterface {
    var json: JsonInterface =
        Json

    override fun fromPulsar(input: PulsarWorkflowMessage): WorkflowMessage {
        return json.parse(
            json.stringify(input), WorkflowMessage::class) as WorkflowMessage
    }

    override fun toPulsar(msg: WorkflowMessage): PulsarWorkflowMessage {
        return json.parse(
            json.stringify(msg), PulsarWorkflowMessage::class) as PulsarWorkflowMessage
    }
}
