package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskInput
import io.infinitic.workflowManager.common.data.workflowTasks.WorkflowTaskOutput

interface WorkflowTask {
    fun handle(input: WorkflowTaskInput): WorkflowTaskOutput
}
