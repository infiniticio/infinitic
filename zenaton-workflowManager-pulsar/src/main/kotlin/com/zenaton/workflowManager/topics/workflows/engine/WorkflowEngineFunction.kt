package com.zenaton.workflowManager.topics.workflows.engine

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.commons.pulsar.utils.StateStorage
import com.zenaton.workflowManager.pulsar.topics.workflows.functions.WorkflowEngineDispatcher
import com.zenaton.workflowManager.pulsar.topics.workflows.messages.WorkflowMessageContainer
import com.zenaton.workflowManager.engine.WorkflowEngine
import com.zenaton.workflowManager.topics.workflows.state.WorkflowState
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEngineFunction : Function<WorkflowMessageContainer, Void> {

    override fun process(input: WorkflowMessageContainer, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        try {
            val msg = input.msg()

            WorkflowEngine(
                stater = StateStorage<WorkflowState>(ctx),
                dispatcher = WorkflowEngineDispatcher(
                    ctx
                ),
                logger = Logger(ctx)
            ).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
        }

        return null
    }
}
