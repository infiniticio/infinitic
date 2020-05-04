package com.zenaton.engine.decisions.data

import com.zenaton.engine.workflows.data.states.Action
import com.zenaton.engine.workflows.data.states.BranchId
import com.zenaton.engine.workflows.data.states.BranchOutput
import com.zenaton.engine.workflows.data.states.Step

data class DecisionsPerBranch(
    val branchId: BranchId,
    val step: Step?,
    val actions: List<Action>,
    val output: BranchOutput?
)
