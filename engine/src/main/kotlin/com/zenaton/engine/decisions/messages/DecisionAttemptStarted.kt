package com.zenaton.engine.decisions.messages

import com.zenaton.engine.decisionAttempts.data.DecisionAttemptId
import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowId

class DecisionAttemptStarted(
    override var decisionId: DecisionId,
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val decisionAttemptId: DecisionAttemptId
) : DecisionInterface
