package io.infinitic.engine.workflowManager.storages

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.states.WorkflowState

open class InMemoryWorkflowStateStorage : WorkflowStateStorage {
    private var workflowStateStore: MutableMap<String, WorkflowState> = mutableMapOf()

    override fun createState(workflowId: WorkflowId, state: WorkflowState) {
        workflowStateStore["$workflowId"] = state
    }

    override fun getState(workflowId: WorkflowId) = workflowStateStore["$workflowId"]

    override fun updateState(workflowId: WorkflowId, state: WorkflowState) {
        workflowStateStore["$workflowId"] = state
    }

    override fun deleteState(workflowId: WorkflowId) {
        workflowStateStore.remove("$workflowId")
    }

    fun reset() {
        workflowStateStore.clear()
    }
}
