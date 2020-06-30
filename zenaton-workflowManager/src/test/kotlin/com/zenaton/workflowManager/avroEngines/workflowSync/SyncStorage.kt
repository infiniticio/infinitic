package com.zenaton.workflowManager.avroEngines.workflowSync

import com.zenaton.workflowManager.avroInterfaces.AvroStorage
import com.zenaton.workflowManager.states.AvroWorkflowEngineState

internal class SyncStorage : AvroStorage {
    var workflowEngineStore: Map<String, AvroWorkflowEngineState> = mapOf()

    fun init() {
        workflowEngineStore = mapOf()
    }

    override fun getWorkflowEngineState(workflowId: String): AvroWorkflowEngineState? {
        return workflowEngineStore[workflowId]
    }

    override fun updateWorkflowEngineState(workflowId: String, newState: AvroWorkflowEngineState, oldState: AvroWorkflowEngineState?) {
        workflowEngineStore = workflowEngineStore.plus(workflowId to newState)
    }

    override fun deleteWorkflowEngineState(workflowId: String) {
        workflowEngineStore = workflowEngineStore.minus(workflowId)
    }
}
