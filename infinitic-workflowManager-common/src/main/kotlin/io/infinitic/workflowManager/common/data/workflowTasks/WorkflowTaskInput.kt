package io.infinitic.workflowManager.common.data.workflowTasks

import io.infinitic.workflowManager.common.data.methodRuns.MethodRun
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowMessageIndex
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions

data class WorkflowTaskInput(
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowOptions: WorkflowOptions,
    val workflowPropertyStore: PropertyStore,
    val workflowMessageIndex: WorkflowMessageIndex,

    val methodRun: MethodRun
)
