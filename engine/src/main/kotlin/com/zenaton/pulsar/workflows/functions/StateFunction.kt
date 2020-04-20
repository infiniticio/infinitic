package com.zenaton.pulsar.workflows.functions

import com.zenaton.engine.workflows.Engine
import com.zenaton.pulsar.workflows.Dispatcher
import com.zenaton.pulsar.workflows.Logger
import com.zenaton.pulsar.workflows.PulsarMessage
import com.zenaton.pulsar.workflows.Stater
import com.zenaton.pulsar.workflows.serializers.MessageSerDe
import com.zenaton.pulsar.workflows.serializers.StateSerDe
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class StateFunction : Function<PulsarMessage, Void> {
    override fun process(input: PulsarMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        Engine(
            stater = Stater(ctx, StateSerDe),
            dispatcher = Dispatcher(ctx, MessageSerDe),
            logger = Logger(ctx, MessageSerDe)
        ).handle(MessageSerDe.fromPulsar(input))

        return null
    }
}
