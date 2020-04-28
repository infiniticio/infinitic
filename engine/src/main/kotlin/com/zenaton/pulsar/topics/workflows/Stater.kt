package com.zenaton.pulsar.topics.workflows

import com.zenaton.engine.data.workflows.WorkflowState
import com.zenaton.engine.topics.workflows.StaterInterface
import com.zenaton.pulsar.topics.workflows.serializers.StateSerDe
import com.zenaton.pulsar.topics.workflows.serializers.StateSerDeInterface
import org.apache.pulsar.functions.api.Context

class Stater(private val context: Context) : StaterInterface {

    // StateSerDe injection
    var serDe: StateSerDeInterface = StateSerDe

    override fun getState(key: String): WorkflowState? {
        return context.getState(key) ?. let { serDe.deserialize(it) }
    }

    override fun createState(state: WorkflowState) {
        context.putState(state.workflowId.id, serDe.serialize(state))
    }

    override fun updateState(state: WorkflowState) {
        context.putState(state.workflowId.id, serDe.serialize(state))
    }

    override fun deleteState(state: WorkflowState) {
        context.deleteState(state.workflowId.id)
    }
}
