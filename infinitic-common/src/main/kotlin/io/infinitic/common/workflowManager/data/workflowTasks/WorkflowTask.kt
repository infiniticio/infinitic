package io.infinitic.common.workflowManager.data.workflowTasks

interface WorkflowTask {
    fun handle(input: WorkflowTaskInput): WorkflowTaskOutput
}
