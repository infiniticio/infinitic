/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

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
