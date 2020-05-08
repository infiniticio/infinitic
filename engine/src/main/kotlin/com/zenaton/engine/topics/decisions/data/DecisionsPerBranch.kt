package com.zenaton.engine.topics.decisions.data

import com.zenaton.engine.topics.workflows.state.Action
import com.zenaton.engine.topics.workflows.state.BranchId
import com.zenaton.engine.topics.workflows.state.BranchOutput
import com.zenaton.engine.topics.workflows.state.Step

data class DecisionsPerBranch(
    val branchId: BranchId,
    val step: Step?,
    val actions: List<Action>,
    val output: BranchOutput?
)
