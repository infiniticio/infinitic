package com.zenaton.engine.topics.decisions.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.decisions.DecisionAttemptError
import com.zenaton.engine.data.decisions.DecisionAttemptId
import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.workflows.WorkflowId

class DecisionAttemptFailed(
    override var decisionId: DecisionId,
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val decisionAttemptId: DecisionAttemptId,
    val decisionAttemptError: DecisionAttemptError
) : DecisionMessageInterface
