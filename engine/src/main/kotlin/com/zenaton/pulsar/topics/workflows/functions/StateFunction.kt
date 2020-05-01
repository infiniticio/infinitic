package com.zenaton.pulsar.topics.workflows.functions

import com.zenaton.engine.topics.workflows.WorkflowEngine
import com.zenaton.pulsar.topics.workflows.WorkflowDispatcher
import com.zenaton.pulsar.topics.workflows.WorkflowStater
import com.zenaton.pulsar.topics.workflows.messages.WorkflowMessageContainer
import com.zenaton.pulsar.utils.Logger
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class StateFunction : Function<WorkflowMessageContainer, Void> {
    // MessageConverter injection

    override fun process(input: WorkflowMessageContainer, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        try {
            val msg = input.msg()

            WorkflowEngine(stater = WorkflowStater(ctx), dispatcher = WorkflowDispatcher(ctx), logger = Logger(ctx)).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
        }

        return null
    }
}
