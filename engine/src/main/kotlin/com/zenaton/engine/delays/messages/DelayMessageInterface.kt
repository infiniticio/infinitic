package com.zenaton.engine.delays.messages

import com.zenaton.engine.delays.data.DelayId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.interfaces.messages.MessageInterface

interface DelayMessageInterface : MessageInterface {
    val delayId: DelayId
    override var receivedAt: DateTime?
    override fun getKey() = delayId.id
}
