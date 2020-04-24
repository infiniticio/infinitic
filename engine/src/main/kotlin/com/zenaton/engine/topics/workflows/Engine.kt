package com.zenaton.engine.topics.workflows

import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.types.DateTime
import com.zenaton.engine.data.workflows.WorkflowState
import com.zenaton.engine.data.workflows.states.Branch
import com.zenaton.engine.topics.decisions.DecisionDispatched

class Engine(
    val stater: StaterInterface,
    val dispatcher: DispatcherInterface,
    val logger: LoggerInterface
) {
    fun handle(msg: WorkflowMessage) {
        var state = stater.getState(msg.getStateKey())
        // timestamp the message
        msg.receivedAt = DateTime()

        if (state == null) {
            // a null state should mean that this workflow is already terminated => all messages others than WorkflowDispatched are ignored
            if (msg !is WorkflowDispatched) {
                logger.warn("No state found for message:%s(It's normal if this workflow is already terminated)", msg)
                return
            }
            // init a state
            state = WorkflowState(workflowId = msg.workflowId)
        } else {
            // this should never happen
            if (state.workflowId != msg.workflowId) {
                logger.error("Inconsistent workflowId in message:%s and State:%s)", msg, state)
                return
            }
            // a non-null state with WorkflowDispatched should mean that this message has been replicated
            if (msg is WorkflowDispatched) {
                logger.error("Already existing state for message:%s", msg)
                return
            }
        }

        if (msg is DecisionCompleted) {
            // check ongoing decision
            if (state.ongoingDecisionId != msg.decisionId) {
                logger.error("Inconsistent decisionId in message:%s and State:%s", msg, state)
                return
            }
            // remove ongoing decision from state
            state.ongoingDecisionId = null
        } else {
            if (state.ongoingDecisionId != null) {
                // buffer this message to handle it after decision returns
                state.bufferedMessages.add(msg)
                // save state
                stater.updateState(state)
                return
            }
        }

        when (msg) {
            is WorkflowDispatched -> dispatchWorkflow(state, msg)
            is DecisionCompleted -> completeDecision(state, msg)
            is TaskCompleted -> completeTask(state, msg)
            is DelayCompleted -> completeDelay(state, msg)
        }
    }

    private fun dispatchWorkflow(state: WorkflowState, msg: WorkflowDispatched) {
        val decisionId = DecisionId()
        // define branch
        val branch = Branch.Handle(workflowData = msg.workflowData)
        // initialize state
        state.ongoingDecisionId = decisionId
        state.runningBranches.add(branch)
        // create DecisionDispatched message
        val m = DecisionDispatched(
            decisionId = decisionId,
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            branches = listOf(branch)
        )
        // dispatch decision
        dispatcher.dispatchDecision(m)
        // save state
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
