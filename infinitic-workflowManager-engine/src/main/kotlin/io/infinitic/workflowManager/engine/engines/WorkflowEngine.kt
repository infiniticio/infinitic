package io.infinitic.workflowManager.engine.engines

import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskInput
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskName
import io.infinitic.taskManager.common.data.TaskOptions
import io.infinitic.taskManager.common.messages.DispatchTask
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskId
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.methods.Branch
import io.infinitic.workflowManager.common.data.methods.MethodId
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.states.WorkflowEngineState
import io.infinitic.workflowManager.engine.storages.WorkflowEngineStateStorage
import io.infinitic.workflowManager.engine.dispatcher.Dispatcher
import io.infinitic.workflowManager.common.messages.CancelWorkflow
import io.infinitic.workflowManager.common.messages.ChildWorkflowCanceled
import io.infinitic.workflowManager.common.messages.ChildWorkflowCompleted
import io.infinitic.workflowManager.common.messages.DecisionCompleted
import io.infinitic.workflowManager.common.messages.DecisionDispatched
import io.infinitic.workflowManager.common.messages.DelayCompleted
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import io.infinitic.workflowManager.common.messages.EventReceived
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage
import io.infinitic.workflowManager.common.messages.TaskCanceled
import io.infinitic.workflowManager.common.messages.TaskCompleted
import io.infinitic.workflowManager.common.messages.TaskDispatched
import io.infinitic.workflowManager.common.messages.WorkflowCanceled
import io.infinitic.workflowManager.common.messages.WorkflowCompleted
import org.slf4j.Logger

class WorkflowEngine {
    companion object {
        const val META_WORKFLOW_ID = "workflowId"
    }

    lateinit var logger: Logger
    lateinit var storage: WorkflowEngineStateStorage
    lateinit var dispatcher: Dispatcher

    suspend fun handle(msg: ForWorkflowEngineMessage) {
        // discard immediately messages that are not processed
        when (msg) {
            is DecisionDispatched -> return
            is TaskDispatched -> return
            is WorkflowCanceled -> return
            is WorkflowCompleted -> return
            else -> Unit
        }

        // get associated state
        val oldState = storage.getState(msg.workflowId)

        // discard message it workflow is already terminated
        if (oldState == null && msg !is DispatchWorkflow) return

        // store message (except DecisionCompleted) if a decision is ongoing
        if (oldState?.currentWorkflowTaskId != null && msg !is DecisionCompleted) {
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

    private suspend fun dispatchWorkflow(msg: DispatchWorkflow): WorkflowEngineState {
        val state = WorkflowEngineState(workflowId = msg.workflowId)
        val workflowTaskId = WorkflowTaskId()
        // define branch
        val branch = Branch(
            workflowMethodId = MethodId(),
            workflowMethod = msg.methodName,
            workflowMethodInput = msg.methodInput,
            propertiesAtStart = Properties(mapOf()),
            pastSteps = listOf()
        )
        // initialize state
        state.currentWorkflowTaskId = workflowTaskId
        state.currentMethodRuns.add(branch)
        // decision input
        val decisionInput = WorkflowTaskInput(
            workflowName = msg.workflowName,
            workflowId = msg.workflowId,
            branches = listOf(branch),
            store = filterStore(state.propertyStore, listOf(branch))
        )
        // dispatch decision
        dispatcher.toDeciders(
            DispatchTask(
                taskId = TaskId(workflowTaskId.id),
                taskName = TaskName(msg.workflowName.name),
                taskInput = TaskInput(decisionInput),
                taskOptions = TaskOptions(),
                taskMeta = TaskMeta().with(META_WORKFLOW_ID, msg.workflowId.id)
            )
        )
        // log event
        dispatcher.toWorkflowEngine(
            DecisionDispatched(
                workflowTaskId = workflowTaskId,
                workflowId = msg.workflowId,
                workflowName = msg.workflowName,
                workflowTaskInput = decisionInput
            )
        )

        return state
    }

    private fun decisionCompleted(state: WorkflowEngineState, msg: DecisionCompleted): WorkflowEngineState {
        TODO()
//        DispatchTask(
//            taskId = msg.taskId,
//            taskName = msg.taskName,
//            taskInput = msg.taskInput,
//            taskMeta = TaskMeta.builder().add("workflowId", msg.workflowId.id).get()
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
            b.pastSteps.map { it.propertiesAfterCompletion }
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
