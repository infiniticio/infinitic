package io.infinitic.workflowManager.engine.storages

import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.engine.avroInterfaces.AvroStorage
import io.infinitic.workflowManager.common.states.WorkflowState

class WorkflowStateStorage(private val avroStorage: AvroStorage) {

    fun createState(workflowId: WorkflowId, state: WorkflowState) {
        avroStorage.createWorkflowState(
            "$workflowId",
            AvroConverter.toStorage(state)
        )
    }

    fun getState(workflowId: WorkflowId): WorkflowState? {
        return avroStorage.getWorkflowState("$workflowId")?.let { AvroConverter.fromStorage(it) }
    }

    fun updateState(workflowId: WorkflowId, state: WorkflowState) {
        avroStorage.updateWorkflowState(
            "$workflowId",
            AvroConverter.toStorage(state)
        )
    }

    fun deleteState(workflowId: WorkflowId) {
        avroStorage.deleteWorkflowState("$workflowId")
    }
}
