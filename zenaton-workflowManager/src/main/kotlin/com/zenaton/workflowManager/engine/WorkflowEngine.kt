package com.zenaton.workflowManager.engine

import com.zenaton.decisionmanager.data.DecisionData
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.data.DecisionName
import com.zenaton.decisionmanager.messages.DecisionDispatched
import com.zenaton.workflowManager.data.DecisionInput
import com.zenaton.workflowManager.interfaces.LoggerInterface
import com.zenaton.workflowManager.interfaces.StaterInterface
import com.zenaton.workflowManager.topics.workflows.interfaces.WorkflowEngineDispatcherInterface
import com.zenaton.workflowManager.messages.ChildWorkflowCompleted
import com.zenaton.workflowManager.messages.DecisionCompleted
import com.zenaton.workflowManager.messages.DelayCompleted
import com.zenaton.workflowManager.messages.DispatchWorkflow
import com.zenaton.workflowManager.messages.EventReceived
import com.zenaton.workflowManager.messages.TaskCompleted
import com.zenaton.workflowManager.messages.WorkflowCompleted
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage
import com.zenaton.workflowManager.topics.workflows.state.Branch
import com.zenaton.workflowManager.topics.workflows.state.Store
import com.zenaton.workflowManager.topics.workflows.state.WorkflowState

class WorkflowEngine(
    val stater: StaterInterface<WorkflowState>,
    val dispatcher: WorkflowEngineDispatcherInterface,
    val logger: LoggerInterface
) {
    fun handle(msg: ForWorkflowEngineMessage) {
        // get associated state
        var state = stater.getState(msg.workflowId.id)
        if (state == null) {
            // a null state should mean that this workflow is already terminated => all messages others than WorkflowDispatched are ignored
            if (msg !is DispatchWorkflow) {
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
            if (msg is DispatchWorkflow) {
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
                stater.updateState(msg.workflowId.id, state)
                return
            }
        }

        when (msg) {
            is DispatchWorkflow -> dispatchWorkflow(state, msg)
            is DecisionCompleted -> completeDecision(state, msg)
            is TaskCompleted -> completeTask(state, msg)
            is ChildWorkflowCompleted -> completeChildWorkflow(state, msg)
            is DelayCompleted -> completeDelay(state, msg)
            is EventReceived -> eventReceived(state, msg)
            is WorkflowCompleted -> workflowCompleted(state, msg)
        }
    }

    private fun dispatchWorkflow(state: WorkflowState, msg: DispatchWorkflow) {
        val decisionId = DecisionId()
        // define branch
        val branch = Branch.Handle(workflowData = msg.workflowData)
        // initialize state
        state.ongoingDecisionId = decisionId
        state.runningBranches.add(branch)
        // create DecisionDispatched message
        val decisionInput = DecisionInput(listOf(branch), filterStore(state.store, listOf(branch)))
        val m = DecisionDispatched(
            decisionId = decisionId,
            workflowId = msg.workflowId,
            decisionName = DecisionName(msg.workflowName.name),
            decisionData = DecisionData("".toByteArray()) // AvroSerDe.serialize(decisionInput))
        )
        // dispatch decision
        dispatcher.dispatch(m)
        // save state
        stater.createState(msg.workflowId.id, state)
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
            b ->
            b.steps.filter { it.propertiesAfterCompletion != null }.map { it.propertiesAfterCompletion!! }
        }
        // Retrieve properties when starting in branches
        val listProperties2 = branches.map {
            b ->
            b.propertiesAtStart
        }
        // Retrieve List<PropertyHash?> relevant for branches
        val listHashes = listProperties1.union(listProperties2).flatMap { it.properties.values }
        // Keep only relevant keys
        val properties = store.properties.filterKeys { listHashes.contains(it) }

        return Store(properties)
    }
}
