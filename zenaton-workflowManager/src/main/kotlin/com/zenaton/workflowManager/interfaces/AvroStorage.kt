package com.zenaton.workflowManager.interfaces

import com.zenaton.workflowManager.states.AvroWorkflowEngineState

interface AvroStorage {
    fun getEngineState(workflowId: String): AvroWorkflowEngineState?
    fun updateEngineState(workflowId: String, newState: AvroWorkflowEngineState, oldState: AvroWorkflowEngineState?)
    fun deleteEngineState(workflowId: String)
}
