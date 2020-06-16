package com.zenaton.decisionmanager.messages

import com.zenaton.decisionmanager.data.DecisionAttemptError
import com.zenaton.decisionmanager.data.DecisionAttemptId
import com.zenaton.decisionmanager.data.DecisionId
import com.zenaton.decisionmanager.messages.interfaces.DecisionAttemptFailingMessageInterface

data class DecisionAttemptFailed(
    override var decisionId: DecisionId,
    override val decisionAttemptId: DecisionAttemptId,
    override val decisionAttemptIndex: Int,
    override val decisionAttemptDelayBeforeRetry: Float,
    val decisionAttemptError: DecisionAttemptError
) : DecisionAttemptFailingMessageInterface
