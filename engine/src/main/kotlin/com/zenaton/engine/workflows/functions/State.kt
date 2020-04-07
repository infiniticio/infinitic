package com.zenaton.engine.workflows.functions

import com.zenaton.engine.messages.Message
import com.zenaton.engine.messages.MessageType
import com.zenaton.engine.messages.WorkflowDispatched
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class State : Function<Message, Void> {
    override fun process(input: Message, context: Context?): Void? {
        when (input.type) {
            MessageType.WORKFLOW_DISPATCHED -> handleWorkflowDispatched(input.get<WorkflowDispatched>())
        }

        return null
    }

    private fun handleWorkflowDispatched(msg: WorkflowDispatched): Unit? {
        return null
    }
}
