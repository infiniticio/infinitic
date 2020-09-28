package io.infinitic.engine.workflowManager.storages

import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.states.WorkflowState

interface WorkflowStateStorage {
    fun createState(workflowId: WorkflowId, state: WorkflowState)

    fun getState(workflowId: WorkflowId): WorkflowState?

    fun updateState(workflowId: WorkflowId, state: WorkflowState)

    fun deleteState(workflowId: WorkflowId)
}
