package com.zenaton.engine.delays.engine

import com.zenaton.engine.delays.data.DelayState
import com.zenaton.engine.delays.messages.DelayCompleted
import com.zenaton.engine.delays.messages.DelayDispatched
import com.zenaton.engine.delays.messages.DelayMessageInterface
import com.zenaton.engine.interfaces.LoggerInterface
import com.zenaton.engine.interfaces.StaterInterface
import com.zenaton.engine.interfaces.data.DateTime

class DelayEngine(
    private val stater: StaterInterface<DelayState>,
    private val dispatcher: DelayEngineDispatcherInterface,
    private val logger: LoggerInterface
) {
    fun handle(msg: DelayMessageInterface) {
        // timestamp the message
        msg.receivedAt = DateTime()
        // get associated state
        var state = stater.getState(msg.getKey())
        if (state == null) {
            // a null state should mean that this delay is already terminated => all messages others than DelayDispatched are ignored
            if (msg !is DelayDispatched) {
                logger.warn("No state found for message:%s(It's normal if this delay is already terminated)", msg)
                return
            }
            // init a state
            state = DelayState(delayId = msg.delayId)
        } else {
            // this should never happen
            if (state.delayId != msg.delayId) {
                logger.error("Inconsistent delayId in message:%s and State:%s)", msg, state)
                return
            }
            // a non-null state with DelayDispatched should mean that this message has been replicated
            if (msg is DelayDispatched) {
                logger.error("Already existing state for message:%s", msg)
                return
            }
        }

        when (msg) {
            is DelayCompleted -> completeDelay(state, msg)
            is DelayDispatched -> dispatchDelay(state, msg)
        }
    }

    private fun completeDelay(state: DelayState, msg: DelayCompleted) {
        TODO()
    }

    private fun dispatchDelay(state: DelayState, msg: DelayDispatched) {
        TODO()
    }
}
