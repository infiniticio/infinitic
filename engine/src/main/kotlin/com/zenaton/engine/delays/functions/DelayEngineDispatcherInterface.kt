package com.zenaton.engine.delays.functions

import com.zenaton.engine.workflows.messages.DelayCompleted

interface DelayEngineDispatcherInterface {
    fun dispatch(msg: DelayCompleted, after: Float = 0f)
}
