package com.zenaton.engine.delays.interfaces

import com.zenaton.engine.delays.data.DelayId
import com.zenaton.engine.interfaces.MessageInterface
import com.zenaton.engine.interfaces.data.DateTime

interface DelayMessageInterface : MessageInterface {
    val delayId: DelayId
    override var receivedAt: DateTime?
    override fun getKey() = delayId.id
}
