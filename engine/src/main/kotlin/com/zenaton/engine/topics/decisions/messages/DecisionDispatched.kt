package com.zenaton.engine.topics.decisions.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.decisions.DecisionId
import com.zenaton.engine.data.workflows.WorkflowId
import com.zenaton.engine.data.workflows.WorkflowName
import com.zenaton.engine.data.workflows.states.Branch
import com.zenaton.engine.data.workflows.states.Store

class DecisionDispatched(
    override var decisionId: DecisionId,
    override var workflowId: WorkflowId,
    override var receivedAt: DateTime? = null,
    val workflowName: WorkflowName,
    val branches: List<Branch>,
    val store: Store
) : DecisionMessageInterface
