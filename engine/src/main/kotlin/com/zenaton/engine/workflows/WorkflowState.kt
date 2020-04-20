package com.zenaton.engine.workflows

import com.zenaton.engine.common.attributes.DecisionId
import com.zenaton.engine.common.attributes.WorkflowId
import com.zenaton.engine.workflows.state.Action
import com.zenaton.engine.workflows.state.ActionId
import com.zenaton.engine.workflows.state.Branch
import com.zenaton.engine.workflows.state.StoreHash

data class WorkflowState(
    val workflowId: WorkflowId,
    val ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: List<WorkflowMessage> = listOf(),
    val store: Map<StoreHash, String> = mapOf(),
    val actions: Map<ActionId, Action> = mapOf(),
    val runningBranches: List<Branch> = listOf()
)
