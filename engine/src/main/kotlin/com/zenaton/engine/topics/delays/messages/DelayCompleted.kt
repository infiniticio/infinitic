package com.zenaton.engine.topics.delays.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.delays.DelayId

data class DelayCompleted(
    override var delayId: DelayId,
    override var receivedAt: DateTime? = null
) : DelayMessageInterface
