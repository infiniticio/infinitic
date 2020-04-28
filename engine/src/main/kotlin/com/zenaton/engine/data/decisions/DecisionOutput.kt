package com.zenaton.engine.data.decisions

import com.zenaton.engine.data.workflows.states.Properties
import com.zenaton.engine.data.workflows.states.Store

data class DecisionOutput(
    val properties: Properties?,
    val decisions: List<DecisionsPerBranch>,
    val newStore: Store
)
