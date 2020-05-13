package com.zenaton.workflowengine.topics.decisions.data

import com.zenaton.workflowengine.topics.workflows.state.Action
import com.zenaton.workflowengine.topics.workflows.state.BranchId
import com.zenaton.workflowengine.topics.workflows.state.BranchOutput
import com.zenaton.workflowengine.topics.workflows.state.Step

data class DecisionsPerBranch(
    val branchId: BranchId,
    val step: Step?,
    val actions: List<Action>,
    val output: BranchOutput?
)
