package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.DecisionId
import com.zenaton.engine.decisions.Message.DecisionDispatched
import com.zenaton.engine.workflows.Message.DecisionCompleted
import com.zenaton.engine.workflows.Message.DelayCompleted
import com.zenaton.engine.workflows.Message.TaskCompleted
import com.zenaton.engine.workflows.Message.WorkflowCompleted
import com.zenaton.engine.workflows.Message.WorkflowDispatched

class Engine(val state: State?, val msg: Message, val dispatcher: Dispatcher) {

    init {
        if (state != null && state.workflowId != msg.workflowId) {
            throw Exception("Inconsistent Message and State")
        }
    }

    fun handle() {
        when (msg) {
            is WorkflowDispatched -> dispatchWorkflow(msg)
            is WorkflowCompleted -> completeWorkflow(msg)
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
        dispatcher.createState(State(
            workflowId = msg.workflowId,
            ongoingDecisionId = m.decisionId
        ))
    }

    private fun completeWorkflow(msg: WorkflowCompleted) {
    }

    private fun completeDecision(msg: DecisionCompleted) {
    }

    private fun completeTask(msg: TaskCompleted) {
    }

    private fun completeDelay(msg: DelayCompleted) {
    }

    private fun logState(msg: WorkflowDispatched) {
    }

    private fun logNoState(msg: Message) {
    }
}
