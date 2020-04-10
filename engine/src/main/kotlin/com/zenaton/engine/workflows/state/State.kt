package com.zenaton.engine.workflows.state

import com.zenaton.engine.workflows.messages.MessageInterface

class State(
    val ongoingDecisionId: String,
    val bufferedMessages: List<MessageInterface>,
    val store: Map<StoreHash, String>,
    val unitSteps: Map<UnitStepId, UnitStep>,
    val runningBranches: List<Branch>
)
