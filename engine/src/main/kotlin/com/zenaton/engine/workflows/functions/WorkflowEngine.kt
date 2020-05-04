package com.zenaton.engine.workflows.functions

import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.decisions.messages.DecisionDispatched
import com.zenaton.engine.events.messages.EventReceived
import com.zenaton.engine.interfaces.LoggerInterface
import com.zenaton.engine.interfaces.StaterInterface
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowState
import com.zenaton.engine.workflows.data.states.Branch
import com.zenaton.engine.workflows.data.states.Store
import com.zenaton.engine.workflows.messages.ChildWorkflowCompleted
import com.zenaton.engine.workflows.messages.DecisionCompleted
import com.zenaton.engine.workflows.messages.DelayCompleted
import com.zenaton.engine.workflows.messages.TaskCompleted
import com.zenaton.engine.workflows.messages.WorkflowCompleted
import com.zenaton.engine.workflows.messages.WorkflowDispatched
import com.zenaton.engine.workflows.messages.WorkflowMessageInterface

class WorkflowEngine(
    val stater: StaterInterface<WorkflowState>,
    val dispatcher: WorkflowEngineDispatcherInterface,
    val logger: LoggerInterface
) {
    fun handle(msg: WorkflowMessageInterface) {
        // timestamp the message
        msg.receivedAt = DateTime()
        // get associated state
        var state = stater.getState(msg.getKey())
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
                stater.updateState(msg.getKey(), state)
                return
            }
        }

        when (msg) {
            is WorkflowDispatched -> dispatchWorkflow(state, msg)
            is DecisionCompleted -> completeDecision(state, msg)
            is TaskCompleted -> completeTask(state, msg)
            is ChildWorkflowCompleted -> completeChildWorkflow(state, msg)
            is DelayCompleted -> completeDelay(state, msg)
            is EventReceived -> eventReceived(state, msg)
            is WorkflowCompleted -> workflowCompleted(state, msg)
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
            branches = listOf(branch),
            store = filterStore(state.store, listOf(branch))
        )
        // dispatch decision
        dispatcher.dispatch(m)
        // save state
        stater.createState(msg.getKey(), state)
    }

    private fun completeDecision(state: WorkflowState, msg: DecisionCompleted) {
        TODO()
    }

    private fun completeTask(state: WorkflowState, msg: TaskCompleted) {
        TODO()
    }

    private fun completeChildWorkflow(state: WorkflowState, msg: ChildWorkflowCompleted) {
        TODO()
    }

    private fun completeDelay(state: WorkflowState, msg: DelayCompleted) {
        TODO()
    }

    private fun eventReceived(state: WorkflowState, msg: EventReceived) {
        TODO()
    }

    private fun workflowCompleted(state: WorkflowState, msg: WorkflowCompleted) {
        TODO()
    }

    private fun filterStore(store: Store, branches: List<Branch>): Store {
        // Retrieve properties at step at completion in branches
        val listProperties1 = branches.flatMap {
            b -> b.steps.filter { it.propertiesAfterCompletion != null }.map { it.propertiesAfterCompletion!! }
        }
        // Retrieve properties when starting in branches
        val listProperties2 = branches.map {
                b -> b.propertiesAtStart
        }
        // Retrieve List<PropertyHash?> relevant for branches
        val listHashes = listProperties1.union(listProperties2).flatMap { it.properties.values }
        // Keep only relevant keys
        val properties = store.properties.filterKeys { listHashes.contains(it) }

        return Store(properties)
    }
}
