package com.zenaton.engine.topics.decisions.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.workflows.WorkflowId

interface DecisionMessageInterface {
    val decisionId: DecisionId
    val workflowId: WorkflowId
    var receivedAt: DateTime?
    fun getKey() = decisionId.id
}
