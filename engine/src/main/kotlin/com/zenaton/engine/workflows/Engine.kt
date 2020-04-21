package com.zenaton.engine.workflows

import com.zenaton.engine.attributes.decisions.DecisionId
import com.zenaton.engine.attributes.types.DateTime
import com.zenaton.engine.attributes.workflows.WorkflowState
import com.zenaton.engine.attributes.workflows.states.Branch
import com.zenaton.engine.decisions.DecisionDispatched

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
        val decisionId = DecisionId()
        // define branch to process
        val branch = Branch.Handle(
            workflowData = msg.workflowData,
            decidedAt = DateTime()
        )
        // build state
        val state = WorkflowState(
            workflowId = msg.workflowId,
            ongoingDecisionId = decisionId,
            runningBranches = listOf(branch)
        )
        // create DecisionDispatched message
        val m = DecisionDispatched(
            decisionId = decisionId,
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            runningBranches = listOf(branch)
        )
        // dispatch decision
        dispatcher.dispatchDecision(m)
        // only then, save state
        stater.createState(state)
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
