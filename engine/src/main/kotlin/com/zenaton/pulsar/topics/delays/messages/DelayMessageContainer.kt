package com.zenaton.pulsar.topics.delays.messages

import com.zenaton.engine.topics.delays.messages.DelayDispatched
import com.zenaton.engine.topics.delays.messages.DelayMessageInterface
import com.zenaton.engine.topics.workflows.messages.DelayCompleted
import kotlin.reflect.full.declaredMemberProperties

class DelayMessageContainer(
    val delayCompleted: DelayCompleted? = null,
    val delayDispatched: DelayDispatched? = null
) {
    fun msg(): DelayMessageInterface {
        // get list of non null properties
        val msg = DelayMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as DelayMessageInterface
    }
}
