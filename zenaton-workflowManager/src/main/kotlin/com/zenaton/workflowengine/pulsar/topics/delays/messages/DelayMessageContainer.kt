package com.zenaton.workflowengine.pulsar.topics.delays.messages

import com.zenaton.workflowengine.topics.delays.interfaces.DelayMessageInterface
import com.zenaton.workflowengine.topics.delays.messages.DelayDispatched
import kotlin.reflect.full.declaredMemberProperties

class DelayMessageContainer {
    var delayDispatched: DelayDispatched? = null

    constructor(msg: DelayMessageInterface) {
        when (msg) {
            is DelayDispatched -> this.delayDispatched = msg
        }
    }

    fun msg(): DelayMessageInterface {
        // get list of non null properties
        val msg = DelayMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as DelayMessageInterface
    }
}
