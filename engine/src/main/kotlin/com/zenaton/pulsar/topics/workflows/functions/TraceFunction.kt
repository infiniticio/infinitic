package com.zenaton.pulsar.topics.workflows.functions

import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TraceFunction : Function<PulsarWorkflowMessage, PulsarWorkflowMessage> {
    override fun process(input: PulsarWorkflowMessage?, context: Context?): PulsarWorkflowMessage? {
        return input
    }
}
