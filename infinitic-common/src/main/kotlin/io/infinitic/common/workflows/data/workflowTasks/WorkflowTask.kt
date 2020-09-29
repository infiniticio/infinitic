package io.infinitic.common.workflows.data.workflowTasks

import io.infinitic.common.tasks.Task

interface WorkflowTask : Task {
    fun handle(input: WorkflowTaskInput): WorkflowTaskOutput
}
