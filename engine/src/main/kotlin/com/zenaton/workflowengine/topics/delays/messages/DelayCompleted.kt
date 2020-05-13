package com.zenaton.workflowengine.topics.delays.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowengine.data.DelayId
import com.zenaton.workflowengine.topics.delays.interfaces.DelayMessageInterface

data class DelayCompleted(
    override var delayId: DelayId,
    override var sentAt: DateTime? = null
) : DelayMessageInterface
