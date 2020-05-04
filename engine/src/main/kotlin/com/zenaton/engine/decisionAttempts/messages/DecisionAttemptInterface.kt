package com.zenaton.engine.decisionAttempts.messages

import com.zenaton.engine.decisionAttempts.data.DecisionAttemptId
import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.interfaces.messages.MessageInterface

interface DecisionAttemptInterface : MessageInterface {
    val decisionId: DecisionId
    val decisionAttemptId: DecisionAttemptId
    override var sentAt: DateTime?
    override var receivedAt: DateTime?
    override fun getKey() = decisionAttemptId.id
}
