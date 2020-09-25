package io.infinitic.common.workflowManager.data.workflowTasks

import io.infinitic.common.workflowManager.data.methodRuns.MethodRun
import io.infinitic.common.workflowManager.data.properties.PropertyStore
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.workflows.WorkflowMessageIndex
import io.infinitic.common.workflowManager.data.workflows.WorkflowName
import io.infinitic.common.workflowManager.data.workflows.WorkflowOptions

data class WorkflowTaskInput(
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowOptions: WorkflowOptions,
    val workflowPropertyStore: PropertyStore,
    val workflowMessageIndex: WorkflowMessageIndex,

    val methodRun: MethodRun
)
