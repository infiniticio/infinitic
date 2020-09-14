package io.infinitic.workflowManager.engine.avroInterfaces

import io.infinitic.workflowManager.states.AvroWorkfloState

interface AvroStorage {
    fun createWorkflowState(workflowId: String, state: AvroWorkfloState)
    fun getWorkflowState(workflowId: String): AvroWorkfloState?
    fun updateWorkflowState(workflowId: String, state: AvroWorkfloState)
    fun deleteWorkflowState(workflowId: String)
}
