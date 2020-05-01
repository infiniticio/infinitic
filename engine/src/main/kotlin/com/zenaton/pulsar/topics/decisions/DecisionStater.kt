package com.zenaton.pulsar.topics.decisions

import com.zenaton.engine.data.decisions.DecisionState
import com.zenaton.engine.topics.decisions.DecisionStaterInterface
import com.zenaton.pulsar.utils.StateSerDe
import com.zenaton.pulsar.utils.StateSerDeInterface
import org.apache.pulsar.functions.api.Context

class DecisionStater(private val context: Context) : DecisionStaterInterface {

    // StateSerDe injection
    var serDe: StateSerDeInterface = StateSerDe

    override fun getState(key: String): DecisionState? {
        return context.getState(key) ?. let { serDe.deserialize(it) }
    }

    override fun createState(state: DecisionState) {
        context.putState(state.getKey(), serDe.serialize(state))
    }

    override fun updateState(state: DecisionState) {
        context.putState(state.getKey(), serDe.serialize(state))
    }

    override fun deleteState(state: DecisionState) {
        context.deleteState(state.getKey())
    }
}
