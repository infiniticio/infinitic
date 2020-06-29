package com.zenaton.workflowManager.engines

import com.zenaton.jobManager.data.JobId
import com.zenaton.jobManager.data.JobInput
import com.zenaton.jobManager.data.JobMeta
import com.zenaton.jobManager.data.JobName
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.workflowManager.avro.AvroConverter
import com.zenaton.workflowManager.data.DecisionId
import com.zenaton.workflowManager.data.DecisionInput
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.branches.BranchName
import com.zenaton.workflowManager.data.properties.PropertyStore
import com.zenaton.workflowManager.states.WorkflowEngineState
import com.zenaton.workflowManager.storages.WorkflowEngineStateStorage
import com.zenaton.workflowManager.dispatcher.Dispatcher
import com.zenaton.workflowManager.messages.CancelWorkflow
import com.zenaton.workflowManager.messages.ChildWorkflowCanceled
import com.zenaton.workflowManager.messages.ChildWorkflowCompleted
import com.zenaton.workflowManager.messages.DecisionCompleted
import com.zenaton.workflowManager.messages.DecisionDispatched
import com.zenaton.workflowManager.messages.DelayCompleted
import com.zenaton.workflowManager.messages.DispatchWorkflow
import com.zenaton.workflowManager.messages.EventReceived
import com.zenaton.workflowManager.messages.ForWorkflowEngineMessage
import com.zenaton.workflowManager.messages.TaskCanceled
import com.zenaton.workflowManager.messages.TaskCompleted
import com.zenaton.workflowManager.messages.TaskDispatched
import com.zenaton.workflowManager.messages.WorkflowCanceled
import com.zenaton.workflowManager.messages.WorkflowCompleted
import org.slf4j.Logger

class WorkflowEngine {
    lateinit var logger: Logger
    lateinit var storage: WorkflowEngineStateStorage
    lateinit var dispatcher: Dispatcher

    fun handle(msg: ForWorkflowEngineMessage) {
        // discard immediately messages that are not processed
        when (msg) {
            is DecisionDispatched -> return
            is TaskDispatched -> return
            is WorkflowCanceled -> return
            is WorkflowCompleted -> return
        }

        // get associated state
        val oldState = storage.getState(msg.workflowId)

        // discard message it workflow is already terminated
        if (oldState == null && msg !is DispatchWorkflow) return

        // store message (except DecisionCompleted) if a decision is ongoing
        if (oldState?.ongoingDecisionId != null && msg !is DecisionCompleted) {
            val newState = bufferMessage(oldState, msg)
            storage.updateState(msg.workflowId, newState, oldState)
            return
        }

        val newState =
            if (oldState == null)
                dispatchWorkflow(msg as DispatchWorkflow)
            else when (msg) {
                is CancelWorkflow -> cancelWorkflow(oldState, msg)
                is ChildWorkflowCanceled -> childWorkflowCanceled(oldState, msg)
                is ChildWorkflowCompleted -> childWorkflowCompleted(oldState, msg)
                is DecisionCompleted -> decisionCompleted(oldState, msg)
                is DelayCompleted -> delayCompleted(oldState, msg)
                is EventReceived -> eventReceived(oldState, msg)
                is TaskCanceled -> taskCanceled(oldState, msg)
                is TaskCompleted -> taskCompleted(oldState, msg)
                else -> throw Exception("Unknown ForWorkflowEngineMessage: ${msg::class.qualifiedName}")
            }

        // store state if modified
        if (newState != oldState) {
            storage.updateState(msg.workflowId, newState, oldState)
        }
    }

    private fun bufferMessage(state: WorkflowEngineState, msg: ForWorkflowEngineMessage): WorkflowEngineState {
        // buffer this message to handle it after decision returns
        // val bufferedMessages = oldState.bufferedMessages.add(msg)
        // oldState.bufferedMessages.add(msg)
        TODO()
    }

    private fun cancelWorkflow(state: WorkflowEngineState, msg: CancelWorkflow): WorkflowEngineState {
        TODO()
    }

    private fun childWorkflowCanceled(state: WorkflowEngineState, msg: ChildWorkflowCanceled): WorkflowEngineState {
        TODO()
    }

    private fun dispatchWorkflow(msg: DispatchWorkflow): WorkflowEngineState {
        val state = WorkflowEngineState(workflowId = msg.workflowId)
        val decisionId = DecisionId()
        // define branch
        val branch = Branch(
            branchName = BranchName("handle"),
            branchInput = msg.workflowInput
        )
        // initialize state
        state.ongoingDecisionId = decisionId
        state.runningBranches.add(branch)
        // decision input
        val decisionInput = DecisionInput(
            branches = listOf(branch),
            store = filterStore(state.store, listOf(branch))
        )
        // dispatch decision
        dispatcher.toDeciders(
            DispatchJob(
                jobId = JobId(decisionId.id),
                jobName = JobName(msg.workflowName.name),
                jobInput = JobInput
                    .builder()
                    .add(AvroConverter.toAvroDecisionInput(decisionInput))
                    .build(),
                jobMeta = JobMeta
                    .builder()
                    .add("workflowId", msg.workflowId.id)
                    .build()
            )
        )
        // log event
        dispatcher.toWorkflowEngine(
            DecisionDispatched(
                decisionId = decisionId,
                workflowId = msg.workflowId,
                workflowName = msg.workflowName,
                decisionInput = decisionInput
            )
        )
        // save state
        storage.updateState(msg.workflowId, state, null)

        return state
    }

    private fun decisionCompleted(state: WorkflowEngineState, msg: DecisionCompleted): WorkflowEngineState {
        TODO()
//        DispatchJob(
//            jobId = msg.taskId,
//            jobName = msg.taskName,
//            jobInput = msg.taskInput,
//            jobMeta = JobMeta.builder().add("workflowId", msg.workflowId.id).get()
//        )
    }

    private fun delayCompleted(state: WorkflowEngineState, msg: DelayCompleted): WorkflowEngineState {
        TODO()
    }

    private fun taskCanceled(state: WorkflowEngineState, msg: TaskCanceled): WorkflowEngineState {
        TODO()
    }

    private fun taskCompleted(state: WorkflowEngineState, msg: TaskCompleted): WorkflowEngineState {
        TODO()
    }

    private fun childWorkflowCompleted(state: WorkflowEngineState, msg: ChildWorkflowCompleted): WorkflowEngineState {
        TODO()
    }

    private fun completeDelay(state: WorkflowEngineState, msg: DelayCompleted): WorkflowEngineState {
        TODO()
    }

    private fun eventReceived(state: WorkflowEngineState, msg: EventReceived): WorkflowEngineState {
        TODO()
    }

    private fun filterStore(store: PropertyStore, branches: List<Branch>): PropertyStore {
        // Retrieve properties at step at completion in branches
        val listProperties1 = branches.flatMap {
            b ->
            b.steps.map { it.propertiesAfterCompletion }
        }
        // Retrieve properties when starting in branches
        val listProperties2 = branches.map {
            b ->
            b.propertiesAtStart
        }
        // Retrieve List<PropertyHash?> relevant for branches
        val listHashes = listProperties1.union(listProperties2).flatMap { it.properties.values }
        // Keep only relevant keys
        val properties = store.properties.filterKeys { listHashes.contains(it) }.toMutableMap()

        return PropertyStore(properties)
    }
}
