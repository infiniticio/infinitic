package com.zenaton.engine.decisions.messages

import com.zenaton.engine.decisions.data.DecisionId
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.workflows.data.WorkflowId
import com.zenaton.engine.workflows.data.WorkflowName
import com.zenaton.engine.workflows.data.states.Branch
import com.zenaton.engine.workflows.data.states.Store

data class DecisionDispatched(
    override var decisionId: DecisionId,
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val workflowName: WorkflowName,
    val branches: List<Branch>,
    val store: Store
) : DecisionMessageInterface
