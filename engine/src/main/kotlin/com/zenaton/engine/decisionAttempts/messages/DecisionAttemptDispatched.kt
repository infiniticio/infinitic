package com.zenaton.engine.decisionAttempts.messages

import com.zenaton.engine.decisionAttempts.data.DecisionAttemptId
import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.data.DateTime

class DecisionAttemptDispatched(
    override var decisionId: DecisionId,
    override val decisionAttemptId: DecisionAttemptId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null
) : DecisionAttemptInterface
