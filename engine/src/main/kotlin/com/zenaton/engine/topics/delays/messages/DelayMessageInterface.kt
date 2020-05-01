package com.zenaton.engine.topics.delays.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.delays.DelayId

interface DelayMessageInterface {
    val delayId: DelayId
    var receivedAt: DateTime?
    fun getKey() = delayId.id
}
