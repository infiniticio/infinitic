package com.zenaton.engine.topics.workflows.state

import com.zenaton.engine.data.DecisionId
import com.zenaton.engine.data.WorkflowId
import com.zenaton.engine.data.interfaces.StateInterface
import com.zenaton.engine.topics.workflows.interfaces.WorkflowMessageInterface

data class WorkflowState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<WorkflowMessageInterface> = mutableListOf(),
    val store: Store = Store(),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties()
) : StateInterface
