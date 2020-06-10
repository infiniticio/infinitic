package com.zenaton.workflowManager.topics.decisions.data

import com.zenaton.workflowManager.topics.workflows.state.Action
import com.zenaton.workflowManager.topics.workflows.state.BranchId
import com.zenaton.workflowManager.topics.workflows.state.BranchOutput
import com.zenaton.workflowManager.topics.workflows.state.Step

data class DecisionsPerBranch(
    val branchId: BranchId,
    val step: Step?,
    val actions: List<Action>,
    val output: BranchOutput?
)
