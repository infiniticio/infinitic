package com.zenaton.workflowengine.topics.delays.interfaces

import com.zenaton.workflowengine.data.DelayId
import com.zenaton.workflowengine.interfaces.MessageInterface

interface DelayMessageInterface : MessageInterface {
    val delayId: DelayId
    override fun getStateId() = delayId.id
}
