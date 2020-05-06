package com.zenaton.engine.delays.messages

import com.zenaton.engine.delays.data.DelayId
import com.zenaton.engine.delays.interfaces.DelayMessageInterface
import com.zenaton.engine.interfaces.data.DateTime

data class DelayCompleted(
    override var delayId: DelayId,
    override var sentAt: DateTime? = null,
    override var receivedAt: DateTime? = null
) : DelayMessageInterface
