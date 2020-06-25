package com.zenaton.workflowManager.pulsar.storage

import com.zenaton.common.avro.AvroSerDe
import com.zenaton.workflowManager.interfaces.AvroStorage
import com.zenaton.workflowManager.states.AvroWorkflowEngineState
import org.apache.pulsar.functions.api.Context

class PulsarAvroStorage(val context: Context) : AvroStorage {
    // serializer injection
    private var avroSerDe = AvroSerDe

    override fun getWorkflowEngineState(workflowId: String): AvroWorkflowEngineState? {
        return context.getState(getEngineStateKey(workflowId))?.let {
            avroSerDe.deserialize<AvroWorkflowEngineState>(it)
        }
    }

    override fun updateWorkflowEngineState(
        workflowId: String,
        newState: AvroWorkflowEngineState,
        oldState: AvroWorkflowEngineState?
    ) {
        context.putState(getEngineStateKey(workflowId), avroSerDe.serialize(newState))
    }

    override fun deleteWorkflowEngineState(workflowId: String) {
        context.deleteState(getEngineStateKey(workflowId))
    }

    fun getEngineStateKey(workflowId: String) = "engine.state.$workflowId"
}
