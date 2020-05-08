package com.zenaton.engine.topics.decisions.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DecisionAttemptError
import com.zenaton.engine.data.DecisionAttemptId
import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.decisions.interfaces.DecisionMessageInterface

data class DecisionAttemptFailed(
    override var decisionId: DecisionId,
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val decisionAttemptId: DecisionAttemptId,
    val decisionAttemptError: DecisionAttemptError
) : DecisionMessageInterface
