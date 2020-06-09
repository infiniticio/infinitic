package com.zenaton.workflowengine.topics.delays.interfaces

import com.zenaton.workflowengine.topics.workflows.messages.DelayCompleted

interface DelayEngineDispatcherInterface {
    fun dispatch(msg: DelayCompleted, after: Float = 0f)
}
