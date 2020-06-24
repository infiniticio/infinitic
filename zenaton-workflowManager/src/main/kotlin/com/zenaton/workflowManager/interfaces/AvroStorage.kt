package com.zenaton.workflowManager.interfaces

import com.zenaton.workflowManager.states.AvroWorkflowEngineState

interface AvroStorage {
    fun getWorkflowEngineState(workflowId: String): AvroWorkflowEngineState?
    fun updateWorkflowEngineState(workflowId: String, newState: AvroWorkflowEngineState, oldState: AvroWorkflowEngineState?)
    fun deleteWorkflowEngineState(workflowId: String)
}
