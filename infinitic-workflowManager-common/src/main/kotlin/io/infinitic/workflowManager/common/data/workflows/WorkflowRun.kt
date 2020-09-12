package io.infinitic.workflowManager.common.data.workflows

import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskIndex

data class WorkflowRun(
    val workflowName: WorkflowName,
    val workflowId: WorkflowId,
    val workflowOptions: WorkflowOptions,
    val workflowPropertyStore: PropertyStore,
    val workflowTaskIndex: WorkflowTaskIndex
)
