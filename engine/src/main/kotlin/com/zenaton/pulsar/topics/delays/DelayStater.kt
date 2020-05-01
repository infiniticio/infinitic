package com.zenaton.pulsar.topics.delays

import com.zenaton.engine.data.delays.DelayState
import com.zenaton.engine.topics.delays.DelayStaterInterface
import com.zenaton.pulsar.utils.StateSerDe
import com.zenaton.pulsar.utils.StateSerDeInterface
import org.apache.pulsar.functions.api.Context

class DelayStater(private val context: Context) : DelayStaterInterface {

    // StateSerDe injection
    var serDe: StateSerDeInterface = StateSerDe

    override fun getState(key: String): DelayState? {
        return context.getState(key) ?. let { serDe.deserialize(it) }
    }

    override fun createState(state: DelayState) {
        context.putState(state.getKey(), serDe.serialize(state))
    }

    override fun updateState(state: DelayState) {
        context.putState(state.getKey(), serDe.serialize(state))
    }

    override fun deleteState(state: DelayState) {
        context.deleteState(state.getKey())
    }
}
