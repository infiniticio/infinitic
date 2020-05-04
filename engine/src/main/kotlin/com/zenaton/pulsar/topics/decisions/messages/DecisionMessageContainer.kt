package com.zenaton.pulsar.topics.decisions.messages

import com.zenaton.engine.decisions.messages.DecisionAttemptCompleted
import com.zenaton.engine.decisions.messages.DecisionAttemptFailed
import com.zenaton.engine.decisions.messages.DecisionAttemptStarted
import com.zenaton.engine.decisions.messages.DecisionDispatched
import com.zenaton.engine.decisions.messages.DecisionMessageInterface
import kotlin.reflect.full.declaredMemberProperties

class DecisionMessageContainer {
    var decisionAttemptCompleted: DecisionAttemptCompleted? = null
    var decisionAttemptFailed: DecisionAttemptFailed? = null
    var decisionAttemptStarted: DecisionAttemptStarted? = null
    var decisionDispatched: DecisionDispatched? = null

    constructor(msg: DecisionMessageInterface) {
        when (msg) {
            is DecisionAttemptCompleted -> this.decisionAttemptCompleted = msg
            is DecisionAttemptFailed -> this.decisionAttemptFailed = msg
            is DecisionAttemptStarted -> decisionAttemptStarted = msg
            is DecisionDispatched -> decisionDispatched = msg
        }
    }
    fun msg(): DecisionMessageInterface {
        // get list of non null properties
        val msg = DecisionMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as DecisionMessageInterface
    }
}
