package com.zenaton.engine.topics.decisionAttempts.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DecisionAttemptId
import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.interfaces.MessageInterface

interface DecisionAttemptMessageInterface : MessageInterface {
    val decisionId: DecisionId
    val decisionAttemptId: DecisionAttemptId
    val decisionAttemptIndex: Int
    fun getName(): String
    override var sentAt: DateTime?
    override fun getKey() = decisionAttemptId.id
}
