package com.zenaton.engine.workflows.messages

import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowId

data class DecisionCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val decisionId: DecisionId
) : WorkflowMessageInterface
