package com.zenaton.engine.data.decisions

import com.zenaton.engine.data.workflows.states.Action
import com.zenaton.engine.data.workflows.states.BranchId
import com.zenaton.engine.data.workflows.states.BranchOutput
import com.zenaton.engine.data.workflows.states.Step

data class DecisionsPerBranch(
    val branchId: BranchId,
    val step: Step?,
    val actions: List<Action>,
    val output: BranchOutput?
)
