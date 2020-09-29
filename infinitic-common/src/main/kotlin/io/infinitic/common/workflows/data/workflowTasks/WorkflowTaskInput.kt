package io.infinitic.common.workflows.data.workflowTasks

import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.properties.PropertyStore
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMessageIndex
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions

data class WorkflowTaskInput(
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowOptions: WorkflowOptions,
    val workflowPropertyStore: PropertyStore,
    val workflowMessageIndex: WorkflowMessageIndex,

    val methodRun: MethodRun
)
