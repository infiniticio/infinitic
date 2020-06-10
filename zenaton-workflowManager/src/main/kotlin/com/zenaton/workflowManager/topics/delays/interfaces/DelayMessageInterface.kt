package com.zenaton.workflowManager.topics.delays.interfaces

import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.interfaces.MessageInterface

interface DelayMessageInterface : MessageInterface {
    val delayId: DelayId
    override fun getStateId() = delayId.id
}
