package io.infinitic.workflowManager.common.data.workflowTasks

import io.infinitic.workflowManager.common.data.methods.MethodRun
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions

data class WorkflowTaskInput(
    val workflowName: WorkflowName,
    val workflowId: WorkflowId,
    val workflowOptions: WorkflowOptions,
    val workflowPropertyStore: PropertyStore,
    val workflowTaskIndex: WorkflowTaskIndex,

    val method: MethodRun
)
