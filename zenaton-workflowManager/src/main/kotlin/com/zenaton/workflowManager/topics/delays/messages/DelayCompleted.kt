package com.zenaton.workflowManager.topics.delays.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.topics.delays.interfaces.DelayMessageInterface

data class DelayCompleted(
    override var delayId: DelayId,
    override var sentAt: DateTime? = null
) : DelayMessageInterface
