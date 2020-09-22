package io.infinitic.workflowManager.engine.storages

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.storage.api.Storage
import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.states.WorkflowState
import io.infinitic.workflowManager.states.AvroWorkflowState

/**
 * This WorkflowStateStorage implementation converts state objects used by the engine to Avro objects, and saves
 * them in a persistent key value storage.
 */
open class AvroKeyValueWorkflowStateStorage(private val storage: Storage) : WorkflowStateStorage {

    override fun createState(workflowId: WorkflowId, state: WorkflowState) {
        AvroConverter.toStorage(state)
            .let { AvroSerDe.serialize(it) }
            .let { storage.putState("$workflowId", it) }
    }

    override fun getState(workflowId: WorkflowId) = storage
        .getState("$workflowId")
        ?.let { AvroSerDe.deserialize<AvroWorkflowState>(it) }
        ?.let { AvroConverter.fromStorage(it) }

    override fun updateState(workflowId: WorkflowId, state: WorkflowState) {
        AvroConverter.toStorage(state)
            .let { AvroSerDe.serialize(it) }
            .let { storage.putState("$workflowId", it) }
    }

    override fun deleteState(workflowId: WorkflowId) {
        storage.deleteState("$workflowId")
    }
}
