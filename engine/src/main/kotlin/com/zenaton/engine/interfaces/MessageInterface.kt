package com.zenaton.engine.interfaces

import com.zenaton.engine.data.DateTime

interface MessageInterface {
    var sentAt: DateTime?
    fun getKey(): String
}
