package com.zenaton.engine.workflows.functions

import com.zenaton.engine.messages.*
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class State : Function<Message, Void> {
    override fun process(input: Message, context: Context?): Void? {
        when(input.type) {
            MessageType.WORKFLOW_DISPATCHED -> handleWorkflowDispatched(Message.get<WorkflowDispatched>())
        }
    }

    fun handleWorkflowDispatched(msg: WorkflowDispatched) {

    }
}

