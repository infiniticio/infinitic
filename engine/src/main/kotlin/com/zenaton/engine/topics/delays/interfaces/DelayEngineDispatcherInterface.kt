package com.zenaton.engine.topics.delays.interfaces

import com.zenaton.engine.topics.workflows.messages.DelayCompleted

interface DelayEngineDispatcherInterface {
    fun dispatch(msg: DelayCompleted, after: Float = 0f)
}
