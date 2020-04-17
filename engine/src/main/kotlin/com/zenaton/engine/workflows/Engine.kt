package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.DecisionId
import com.zenaton.engine.decisions.Message.DecisionDispatched
import com.zenaton.engine.workflows.messages.DecisionCompleted
import com.zenaton.engine.workflows.messages.DelayCompleted
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.engine.workflows.messages.WorkflowMessage

class Engine(val dispatcher: DispatcherInterface, val msg: WorkflowMessage) {

    init {
        val state = dispatcher.getState(msg.workflowId.id)

        if (state != null && state.workflowId != msg.workflowId) {
            throw Exception("Inconsistent Message and State")
        }
    }

    fun handle() {
        when (msg) {
            is WorkflowDispatched -> dispatchWorkflow(msg)
            is DecisionCompleted -> completeDecision(msg)
            is TaskCompleted -> completeTask(msg)
            is DelayCompleted -> completeDelay(msg)
        }
    }

    private fun dispatchWorkflow(msg: WorkflowDispatched) {
        val m = DecisionDispatched(
            decisionId = DecisionId(),
            workflowId = msg.workflowId,
            workflowName = msg.workflowName
        )
        dispatcher.dispatchDecision(m)

        dispatcher.updateState(WorkflowState(
            workflowId = msg.workflowId,
            ongoingDecisionId = m.decisionId
        ))
    }

    private fun completeDecision(msg: DecisionCompleted) {
    }

    private fun completeTask(msg: TaskCompleted) {
    }

    private fun completeDelay(msg: DelayCompleted) {
    }

    private fun logState(msg: WorkflowDispatched) {
    }

    private fun logNoState(msg: WorkflowMessage) {
    }
}
