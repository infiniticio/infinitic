package com.zenaton.engine.attributes.workflows

import com.zenaton.engine.attributes.decisions.DecisionId
import com.zenaton.engine.attributes.workflows.states.Branch
import com.zenaton.engine.attributes.workflows.states.Properties
import com.zenaton.engine.attributes.workflows.states.Store
import com.zenaton.engine.workflows.WorkflowMessage

data class WorkflowState(
    val workflowId: WorkflowId,
    val ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: List<WorkflowMessage> = listOf(),
    /**
     *
     */
    val store: Store = Store(),
    val runningBranches: List<Branch>,
    val currentProperties: Properties = Properties()
)
