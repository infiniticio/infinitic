package com.zenaton.workflowManager.data

import com.zenaton.workflowManager.topics.workflows.state.Branch
import com.zenaton.workflowManager.topics.workflows.state.Store

class DecisionInput(
    val branches: List<Branch>,
    val store: Store
)
