package com.zenaton.engine.interfaces.messages

import com.zenaton.engine.interfaces.data.DateTime

interface MessageInterface {
    var sentAt: DateTime?
    var receivedAt: DateTime?
    fun getKey(): String
}
