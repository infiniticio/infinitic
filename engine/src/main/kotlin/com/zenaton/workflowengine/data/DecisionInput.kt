package com.zenaton.workflowengine.data

import com.zenaton.workflowengine.topics.workflows.state.Branch
import com.zenaton.workflowengine.topics.workflows.state.Store

class DecisionInput(
    val branches: List<Branch>,
    val store: Store
)
