package io.infinitic.common.workers.registry

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.tasks.Task
import io.infinitic.workflows.Workflow
import org.jetbrains.annotations.TestOnly

class WorkerRegistry(val name: String?) {
    val tasks = mutableMapOf<TaskName, RegisteredTask>()
    val taskTags = mutableMapOf<TaskName, RegisteredTaskTag>()

    val workflows = mutableMapOf<WorkflowName, RegisteredWorkflow>()
    val workflowTags = mutableMapOf<WorkflowName, RegisteredWorkflowTag>()
    val workflowEngines = mutableMapOf<WorkflowName, RegisteredWorkflowEngine>()

    fun getTaskInstance(taskName: TaskName): Task =
        (tasks[taskName] ?: thisShouldNotHappen()).factory()

    fun getWorkflowInstance(workflowName: WorkflowName): Workflow =
        (workflows[workflowName] ?: thisShouldNotHappen()).factory()

    @TestOnly
    fun flush() {
        taskTags.values.forEach { it.storage.flush() }
        workflowTags.values.forEach { it.storage.flush() }
        workflowEngines.values.forEach { it.storage.flush() }
    }
}
