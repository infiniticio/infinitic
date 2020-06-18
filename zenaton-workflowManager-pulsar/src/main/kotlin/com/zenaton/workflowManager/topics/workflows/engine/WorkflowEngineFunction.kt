package com.zenaton.workflowManager.topics.workflows.engine

import com.zenaton.common.pulsar.utils.Logger
import com.zenaton.common.pulsar.utils.StateStorage
import com.zenaton.workflowManager.pulsar.topics.workflows.functions.WorkflowEngineDispatcher
import com.zenaton.workflowManager.pulsar.topics.workflows.messages.WorkflowMessageContainer
import com.zenaton.workflowManager.engine.WorkflowEngine
import com.zenaton.workflowManager.engine.WorkflowEngineState
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEngineFunction : Function<WorkflowMessageContainer, Void> {

    override fun process(input: WorkflowMessageContainer, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        try {
            val msg = input.msg()

            WorkflowEngine(
                stater = StateStorage<WorkflowEngineState>(ctx),
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
