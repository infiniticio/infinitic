package com.zenaton.workflowManager.topics.decisions.data

import com.zenaton.workflowManager.topics.workflows.state.Properties
import com.zenaton.workflowManager.topics.workflows.state.Store

data class DecisionOutput(
    val properties: Properties?,
    val decisions: List<DecisionsPerBranch>,
    val newStore: Store
)
