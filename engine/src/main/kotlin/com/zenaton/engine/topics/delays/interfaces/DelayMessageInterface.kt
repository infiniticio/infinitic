package com.zenaton.engine.topics.delays.interfaces

import com.zenaton.engine.data.DelayId
import com.zenaton.engine.interfaces.MessageInterface

interface DelayMessageInterface : MessageInterface {
    val delayId: DelayId
    override fun getKey() = delayId.id
}
