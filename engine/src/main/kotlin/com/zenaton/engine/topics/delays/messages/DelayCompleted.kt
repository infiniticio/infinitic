package com.zenaton.engine.topics.delays.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DelayId
import com.zenaton.engine.topics.delays.interfaces.DelayMessageInterface

data class DelayCompleted(
    override var delayId: DelayId,
    override var sentAt: DateTime? = null
) : DelayMessageInterface
