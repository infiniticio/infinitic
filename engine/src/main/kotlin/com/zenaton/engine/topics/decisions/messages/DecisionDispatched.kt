package com.zenaton.engine.topics.decisions.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.WorkflowName
import com.zenaton.engine.topics.decisions.interfaces.DecisionMessageInterface
import com.zenaton.engine.topics.workflows.state.Branch
import com.zenaton.engine.topics.workflows.state.Store

data class DecisionDispatched(
    override var decisionId: DecisionId,
    override var workflowId: WorkflowId,
    override var sentAt: DateTime? = DateTime(),
    val workflowName: WorkflowName,
    val branches: List<Branch>,
    val store: Store
) : DecisionMessageInterface
