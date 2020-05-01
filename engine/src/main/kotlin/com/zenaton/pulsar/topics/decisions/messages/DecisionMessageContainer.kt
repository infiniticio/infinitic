package com.zenaton.pulsar.topics.decisions.messages

import com.zenaton.engine.topics.decisions.messages.DecisionAttemptCompleted
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptDispatched
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptFailed
import com.zenaton.engine.topics.decisions.messages.DecisionAttemptStarted
import com.zenaton.engine.topics.decisions.messages.DecisionDispatched
import com.zenaton.engine.topics.decisions.messages.DecisionMessageInterface
import com.zenaton.engine.topics.workflows.messages.DecisionCompleted
import kotlin.reflect.full.declaredMemberProperties

class DecisionMessageContainer(
    val decisionAttemptCompleted: DecisionAttemptCompleted? = null,
    val decisionAttemptDispatched: DecisionAttemptDispatched? = null,
    val decisionAttemptFailed: DecisionAttemptFailed? = null,
    val decisionAttemptStarted: DecisionAttemptStarted? = null,
    val decisionCompleted: DecisionCompleted? = null,
    val decisionDispatched: DecisionDispatched? = null
) {
    fun msg(): DecisionMessageInterface {
        // get list of non null properties
        val msg = DecisionMessageContainer::class.declaredMemberProperties.mapNotNull { it.get(this) }
        // check we have exactly one property
        if (msg.size != 1) throw Exception("${this::class.qualifiedName} must contain exactly one message, ${msg.size} found")
        // return it
        return msg.first() as DecisionMessageInterface
    }
}
