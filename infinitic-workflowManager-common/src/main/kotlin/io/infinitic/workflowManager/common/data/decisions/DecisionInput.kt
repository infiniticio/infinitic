package io.infinitic.workflowManager.common.data.decisions

import io.infinitic.workflowManager.common.data.branches.Branch
import io.infinitic.workflowManager.common.data.properties.PropertyStore

data class DecisionInput(
    val branches: List<Branch>,
    val store: PropertyStore
)
