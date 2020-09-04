package io.infinitic.workflowManager.common.data.workflowTasks

import io.infinitic.workflowManager.common.data.branches.Branch
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName

data class WorkflowTaskInput(
    val workflowName: WorkflowName,
    val workflowId: WorkflowId,
    val branches: List<Branch>,
    val store: PropertyStore
)
