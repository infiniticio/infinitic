package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.workflows.WorkflowId

data class DecisionCompleted(
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val decisionId: DecisionId
) : WorkflowMessageInterface
