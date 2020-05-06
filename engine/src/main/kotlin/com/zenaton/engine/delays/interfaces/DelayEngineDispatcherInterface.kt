package com.zenaton.engine.delays.interfaces

import com.zenaton.engine.workflows.messages.DelayCompleted

interface DelayEngineDispatcherInterface {
    fun dispatch(msg: DelayCompleted, after: Float = 0f)
}
