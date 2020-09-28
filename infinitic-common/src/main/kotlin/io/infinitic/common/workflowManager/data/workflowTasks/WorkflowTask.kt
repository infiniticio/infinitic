package io.infinitic.common.workflowManager.data.workflowTasks

import io.infinitic.common.taskManager.Task

interface WorkflowTask : Task {
    fun handle(input: WorkflowTaskInput): WorkflowTaskOutput
}
