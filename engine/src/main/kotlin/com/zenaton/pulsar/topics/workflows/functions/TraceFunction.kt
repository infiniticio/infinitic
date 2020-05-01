package com.zenaton.pulsar.topics.workflows.functions

import com.zenaton.pulsar.topics.workflows.messages.WorkflowMessageContainer
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TraceFunction : Function<WorkflowMessageContainer, WorkflowMessageContainer> {
    override fun process(input: WorkflowMessageContainer?, context: Context?): WorkflowMessageContainer? {
        return input
    }
}
