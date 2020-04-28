package com.zenaton.pulsar.topics.workflows.functions

import com.zenaton.engine.topics.workflows.Engine
import com.zenaton.pulsar.topics.workflows.Dispatcher
import com.zenaton.pulsar.topics.workflows.Stater
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessage
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessageConverter
import com.zenaton.pulsar.topics.workflows.messages.PulsarWorkflowMessageConverterInterface
import com.zenaton.pulsar.utils.Logger
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class StateFunction : Function<PulsarWorkflowMessage, Void> {
    // MessageConverter injection
    var converter: PulsarWorkflowMessageConverterInterface =
        PulsarWorkflowMessageConverter

    override fun process(input: PulsarWorkflowMessage, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from workflows.StateFunction")

        try {
            val msg = converter.fromPulsar(input)

            Engine(stater = Stater(ctx), dispatcher = Dispatcher(ctx), logger = Logger(ctx)).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
        }

        return null
    }
}
