package com.zenaton.workflowengine.pulsar.topics.delays.functions

import com.zenaton.commons.pulsar.utils.Logger
import com.zenaton.commons.pulsar.utils.Stater
import com.zenaton.workflowengine.pulsar.topics.delays.messages.DelayMessageContainer
import com.zenaton.workflowengine.topics.delays.engine.DelayEngine
import com.zenaton.workflowengine.topics.delays.state.DelayState
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class DelayEngineFunction : Function<DelayMessageContainer, Void> {

    override fun process(input: DelayMessageContainer, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received from delays.StateFunction")

        try {
            val msg = input.msg()

            DelayEngine(
                stater = Stater<DelayState>(ctx),
                dispatcher = DelayEngineDispatcher(ctx),
                logger = Logger(ctx)
            ).handle(msg)
        } catch (e: Exception) {
            Logger(ctx).error("Error:%s for message:%s", e, input)
        }

        return null
    }
}
