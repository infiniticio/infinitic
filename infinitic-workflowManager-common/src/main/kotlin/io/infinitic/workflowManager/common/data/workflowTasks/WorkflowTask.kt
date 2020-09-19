package io.infinitic.workflowManager.common.data.workflowTasks

interface WorkflowTask {
    fun handle(input: WorkflowTaskInput): WorkflowTaskOutput
}
