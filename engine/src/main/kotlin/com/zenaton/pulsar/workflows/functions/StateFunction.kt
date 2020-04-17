package com.zenaton.pulsar.workflows.functions

import com.zenaton.engine.workflows.Engine
import com.zenaton.pulsar.serializer.MessageSerDe
import com.zenaton.pulsar.workflows.Dispatcher
import com.zenaton.pulsar.workflows.PulsarMessage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class StateFunction : Function<PulsarMessage, Void> {
    override fun process(input: PulsarMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        Engine(dispatcher = Dispatcher(ctx), msg = MessageSerDe.fromPulsar(input)).handle()

        return null
    }
}
