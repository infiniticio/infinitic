package com.zenaton.engine.topics.delays

import com.zenaton.engine.topics.delays.messages.DelayDispatched

interface DelayDispatcherInterface {
    fun dispatchDelay(msg: DelayDispatched)
}
