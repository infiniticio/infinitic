package com.zenaton.decisionmanager.pulsar.stater

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.decisionmanager.pulsar.avro.AvroConverter
import com.zenaton.decisionmanager.state.DecisionState
import com.zenaton.decisionmanager.states.AvroDecisionState
import com.zenaton.workflowengine.interfaces.StaterInterface
import org.apache.pulsar.functions.api.Context

/**
 * This class provides methods to access decision's state
 */
class DecisionStater(private val context: Context) : StaterInterface<DecisionState> {
    // serializer injection
    var avroSerDe = AvroSerDe
    // converter injection
    var avroConverter = AvroConverter

    override fun getState(key: String): DecisionState? {
        return context.getState(key)?. let { avroConverter.fromAvro(avroSerDe.deserialize(it, AvroDecisionState::class)) }
    }

    override fun createState(key: String, state: DecisionState) {
        context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))
    }

    override fun updateState(key: String, state: DecisionState) {
        context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))
    }

    override fun deleteState(key: String) {
        context.deleteState(key)
    }
}
