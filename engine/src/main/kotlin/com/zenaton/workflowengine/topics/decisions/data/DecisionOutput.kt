package com.zenaton.workflowengine.topics.decisions.data

import com.zenaton.workflowengine.topics.workflows.state.Properties
import com.zenaton.workflowengine.topics.workflows.state.Store

data class DecisionOutput(
    val properties: Properties?,
    val decisions: List<DecisionsPerBranch>,
    val newStore: Store
)
