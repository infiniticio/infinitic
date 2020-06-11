package com.zenaton.workflowManager.data

import com.zenaton.workflowManager.data.state.Branch
import com.zenaton.workflowManager.data.state.Store

class DecisionInput(
    val branches: List<Branch>,
    val store: Store
)
