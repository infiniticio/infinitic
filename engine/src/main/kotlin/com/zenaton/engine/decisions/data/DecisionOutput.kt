package com.zenaton.engine.decisions.data

import com.zenaton.engine.workflows.data.states.Properties
import com.zenaton.engine.workflows.data.states.Store

data class DecisionOutput(
    val properties: Properties?,
    val decisions: List<DecisionsPerBranch>,
    val newStore: Store
)
