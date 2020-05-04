package com.zenaton.engine.decisions.messages

import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.MessageInterface
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowId

interface DecisionMessageInterface : MessageInterface {
    val decisionId: DecisionId
    val workflowId: WorkflowId
    override var receivedAt: DateTime?
    override fun getKey() = decisionId.id
}
