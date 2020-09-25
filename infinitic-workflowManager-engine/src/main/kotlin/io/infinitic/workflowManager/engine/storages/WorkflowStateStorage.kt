package io.infinitic.workflowManager.engine.storages

import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.states.WorkflowState

interface WorkflowStateStorage {
    fun createState(workflowId: WorkflowId, state: WorkflowState)

    fun getState(workflowId: WorkflowId): WorkflowState?

    fun updateState(workflowId: WorkflowId, state: WorkflowState)

    fun deleteState(workflowId: WorkflowId)
}
