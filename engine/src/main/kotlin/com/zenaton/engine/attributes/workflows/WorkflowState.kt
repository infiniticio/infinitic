package com.zenaton.engine.attributes.workflows

import com.zenaton.engine.attributes.decisions.DecisionId
import com.zenaton.engine.attributes.workflows.states.Branch
import com.zenaton.engine.attributes.workflows.states.Store
import com.zenaton.engine.attributes.workflows.states.StoreHash
import com.zenaton.engine.workflows.WorkflowMessage

data class WorkflowState(
    val workflowId: WorkflowId,
    val ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: List<WorkflowMessage> = listOf(),
    val stores: Map<StoreHash, Store> = mapOf(),
    val runningBranches: List<Branch> = listOf()
)
