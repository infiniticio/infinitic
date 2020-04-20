package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.DecisionId
import com.zenaton.engine.decisions.DecisionDispatched
import com.zenaton.engine.workflows.messages.DecisionCompleted
import com.zenaton.engine.workflows.messages.DelayCompleted
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.engine.workflows.messages.WorkflowMessage

class Engine(
    val stater: StaterInterface,
    val dispatcher: DispatcherInterface,
    val logger: LoggerInterface
) {
    fun handle(msg: WorkflowMessage) {
        val state = stater.getState(msg.getStateKey())

        if (state == null) {
            if (msg !is WorkflowDispatched) {
                logger.warn("No state found for: ", msg)
                return
            }
        } else {
            if (state.workflowId != msg.workflowId) {
                val message = logger.error("Inconsistent Message and State for: ", msg)
                throw Exception(message)
            }

            if (msg is WorkflowDispatched) {
                logger.warn("Already existing state for: ", msg)
                return
            }
        }

        when (msg) {
            is WorkflowDispatched -> dispatchWorkflow(msg)
            is DecisionCompleted -> state?.let { completeDecision(it, msg) }
            is TaskCompleted -> state?.let { completeTask(it, msg) }
            is DelayCompleted -> state?.let { completeDelay(it, msg) }
        }
    }

    private fun dispatchWorkflow(msg: WorkflowDispatched) {

        val m = DecisionDispatched(
            decisionId = DecisionId(),
            workflowId = msg.workflowId,
            workflowName = msg.workflowName
        )
        dispatcher.dispatchDecision(m)

        stater.createState(WorkflowState(
            workflowId = msg.workflowId,
            ongoingDecisionId = m.decisionId
        ))
    }

    private fun completeDecision(state: WorkflowState, msg: DecisionCompleted) {
        //
    }

    private fun completeTask(state: WorkflowState, msg: TaskCompleted) {
        //
    }

    private fun completeDelay(state: WorkflowState, msg: DelayCompleted) {
        //
    }
}
