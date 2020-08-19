package io.infinitic.workflowManager.engine.data.decisions

import io.infinitic.workflowManager.engine.data.branches.Branch
import io.infinitic.workflowManager.engine.data.properties.PropertyStore

data class DecisionInput(
    val branches: List<Branch>,
    val store: PropertyStore
)
