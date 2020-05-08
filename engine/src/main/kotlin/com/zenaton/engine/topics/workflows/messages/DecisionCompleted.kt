package com.zenaton.engine.topics.workflows.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.topics.workflows.interfaces.WorkflowMessageInterface

data class DecisionCompleted(
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val decisionId: DecisionId
) : WorkflowMessageInterface
