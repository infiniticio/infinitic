package com.zenaton.engine.topics.decisions.data

import com.zenaton.engine.topics.workflows.state.Properties
import com.zenaton.engine.topics.workflows.state.Store

data class DecisionOutput(
    val properties: Properties?,
    val decisions: List<DecisionsPerBranch>,
    val newStore: Store
)
