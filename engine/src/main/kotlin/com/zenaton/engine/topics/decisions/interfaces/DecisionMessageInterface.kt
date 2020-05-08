package com.zenaton.engine.topics.decisions.interfaces

import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.interfaces.MessageInterface

interface DecisionMessageInterface : MessageInterface {
    val decisionId: DecisionId
    val workflowId: WorkflowId
    override fun getKey() = decisionId.id
}
