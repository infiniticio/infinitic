package com.zenaton.pulsar.topics.workflows.messages

import com.zenaton.engine.topics.workflows.WorkflowMessage

interface PulsarWorkflowMessageConverterInterface {
    fun fromPulsar(input: PulsarWorkflowMessage): WorkflowMessage
    fun toPulsar(msg: WorkflowMessage): PulsarWorkflowMessage
}
