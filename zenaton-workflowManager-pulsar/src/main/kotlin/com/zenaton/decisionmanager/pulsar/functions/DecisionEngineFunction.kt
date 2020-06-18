package com.zenaton.decisionmanager.pulsar.functions

import com.zenaton.common.pulsar.utils.Logger
import com.zenaton.decisionmanager.engine.DecisionEngine
import com.zenaton.decisionmanager.messages.AvroDecisionMessage
import com.zenaton.decisionmanager.pulsar.avro.AvroConverter
import com.zenaton.decisionmanager.pulsar.stater.DecisionStater
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

/**
 * This class provides the function used to trigger DecisionEngine from the decisions topic
 */
class DecisionEngineFunction : Function<AvroDecisionMessage, Void> {

    override fun process(input: AvroDecisionMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from decisions.StateFunction")

        try {
            val msg = AvroConverter.fromAvro(input)

            DecisionEngine(
                stater = DecisionStater(ctx),
                dispatcher = DecisionEngineDispatcher(ctx),
                logger = Logger(ctx)
            ).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}
