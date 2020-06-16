package com.zenaton.decisionmanager.messages

import com.zenaton.decisionmanager.data.DecisionAttemptId
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.messages.interfaces.DecisionAttemptFailingMessageInterface

data class DecisionAttemptStarted(
    override var decisionId: DecisionId,
    override val decisionAttemptId: DecisionAttemptId,
    override val decisionAttemptIndex: Int,
    override val decisionAttemptDelayBeforeRetry: Float,
    val decisionAttemptDelayBeforeTimeout: Float
) : DecisionAttemptFailingMessageInterface
