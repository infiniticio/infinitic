package com.zenaton.workflowManager.storages

import com.zenaton.workflowManager.avroConverter.AvroConverter
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.avroInterfaces.AvroStorage
import com.zenaton.workflowManager.states.WorkflowEngineState

class WorkflowEngineStateStorage(val avroStorage: AvroStorage) {

    fun getState(workflowId: WorkflowId): WorkflowEngineState? {
        return avroStorage.getWorkflowEngineState(workflowId.id)?.let { AvroConverter.fromStorage(it) }
    }

    fun updateState(workflowId: WorkflowId, newState: WorkflowEngineState, oldState: WorkflowEngineState?) {
        avroStorage.updateWorkflowEngineState(
            workflowId.id,
            AvroConverter.toStorage(newState),
            oldState?.let { AvroConverter.toStorage(it) }
        )
    }

    fun deleteState(workflowId: WorkflowId) {
        avroStorage.deleteWorkflowEngineState(workflowId.id)
    }
}
