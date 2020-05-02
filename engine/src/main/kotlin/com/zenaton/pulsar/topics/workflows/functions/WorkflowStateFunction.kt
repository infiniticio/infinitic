package com.zenaton.pulsar.topics.workflows.functions

import com.zenaton.engine.data.workflows.WorkflowState
import com.zenaton.engine.topics.workflows.WorkflowEngine
import com.zenaton.pulsar.topics.workflows.WorkflowDispatcher
import com.zenaton.pulsar.topics.workflows.messages.WorkflowMessageContainer
import com.zenaton.pulsar.utils.Logger
import com.zenaton.pulsar.utils.Stater
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowStateFunction : Function<WorkflowMessageContainer, Void> {

    override fun process(input: WorkflowMessageContainer, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        try {
            val msg = input.msg()

            WorkflowEngine(stater = Stater<WorkflowState>(ctx), dispatcher = WorkflowDispatcher(ctx), logger = Logger(ctx)).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
        }

        return null
    }
}
