package io.infinitic.workflowManager.engine.avroInterfaces

import io.infinitic.workflowManager.states.AvroWorkflowEngineState

interface AvroStorage {
    fun getWorkflowEngineState(workflowId: String): AvroWorkflowEngineState?
    fun updateWorkflowEngineState(workflowId: String, newState: AvroWorkflowEngineState, oldState: AvroWorkflowEngineState?)
    fun deleteWorkflowEngineState(workflowId: String)
}
