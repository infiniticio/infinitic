package com.zenaton.pulsar.workflows

import com.zenaton.engine.workflows.StaterInterface
import com.zenaton.engine.workflows.WorkflowState
import com.zenaton.pulsar.serializer.MessageSerDeInterface
import org.apache.pulsar.functions.api.Context

class Stater(private val context: Context, private val serde: MessageSerDeInterface) : StaterInterface {

    override fun getState(key: String): WorkflowState? {
        return context.getState(key) ?. let { serde.deSerializeState(it) }
    }

    override fun createState(state: WorkflowState) {
        context.putState(state.workflowId.id, serde.serializeState(state))
    }

    override fun updateState(state: WorkflowState) {
        context.putState(state.workflowId.id, serde.serializeState(state))
    }

    override fun deleteState(state: WorkflowState) {
        context.deleteState(state.workflowId.id)
    }
}
