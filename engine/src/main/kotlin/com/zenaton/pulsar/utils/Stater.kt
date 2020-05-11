package com.zenaton.pulsar.utils

import com.zenaton.engine.data.interfaces.StateInterface
import com.zenaton.engine.interfaces.StaterInterface
import org.apache.pulsar.functions.api.Context

class Stater<T : StateInterface>(private val context: Context) :
    StaterInterface<T> {

    // serializer injection
    var serDe = AvroSerDe

    override fun getState(key: String): T? {
        return null // context.getState(key) ?. let { serDe.deserialize(it) }
    }

    override fun createState(key: String, state: T) {
//        context.putState(key, serDe.serialize(state))
    }

    override fun updateState(key: String, state: T) {
//        context.putState(key, serDe.serialize(state))
    }

    override fun deleteState(key: String) {
        context.deleteState(key)
    }
}
