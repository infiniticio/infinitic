package com.zenaton.pulsar.workflows.functions

import com.zenaton.engine.topics.workflows.Engine
import com.zenaton.pulsar.workflows.Dispatcher
import com.zenaton.pulsar.workflows.Logger
import com.zenaton.pulsar.workflows.PulsarMessage
import com.zenaton.pulsar.workflows.Stater
import com.zenaton.pulsar.workflows.serializers.MessageConverter
import com.zenaton.pulsar.workflows.serializers.MessageConverterInterface
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class StateFunction : Function<PulsarMessage, Void> {
    // MessageConverter injection
    var converter: MessageConverterInterface = MessageConverter

    override fun process(input: PulsarMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        val msg = converter.fromPulsar(input)

        Engine(
            stater = Stater(ctx),
            dispatcher = Dispatcher(ctx),
            logger = Logger(ctx)
        ).handle(msg)

        return null
    }
}
