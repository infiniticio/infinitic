package com.zenaton.workflowManager.topics.delays.interfaces

import com.zenaton.workflowManager.messages.DelayCompleted

interface DelayEngineDispatcherInterface {
    fun dispatch(msg: DelayCompleted, after: Float = 0f)
}
