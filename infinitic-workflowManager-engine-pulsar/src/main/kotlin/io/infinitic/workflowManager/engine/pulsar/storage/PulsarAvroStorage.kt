package io.infinitic.workflowManager.pulsar.storage

import io.infinitic.common.avro.AvroSerDe
import io.infinitic.workflowManager.engine.avroInterfaces.AvroStorage
import io.infinitic.workflowManager.states.AvroWorkfloState
import org.apache.pulsar.functions.api.Context

class PulsarAvroStorage(val context: Context) : AvroStorage {
    // serializer injection
    private var avroSerDe = AvroSerDe

    override fun getWorkflowState(workflowId: String): AvroWorkfloState? {
        return context.getState(getEngineStateKey(workflowId))?.let {
            avroSerDe.deserialize<AvroWorkfloState>(it)
        }
    }

    override fun updateWorkflowState(
            workflowId: String,
            newState: AvroWorkfloState,
            oldState: AvroWorkfloState?
    ) {
        context.putState(getEngineStateKey(workflowId), avroSerDe.serialize(newState))
    }

    override fun deleteWorkflowState(workflowId: String) {
        context.deleteState(getEngineStateKey(workflowId))
    }

    fun getEngineStateKey(workflowId: String) = "engine.state.$workflowId"
}
