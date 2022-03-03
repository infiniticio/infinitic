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

package io.infinitic.tasks.executor.register

import io.infinitic.common.workers.TaskFactory
import io.infinitic.common.workers.WorkerRegister
import io.infinitic.common.workers.WorkflowFactory
import io.infinitic.exceptions.tasks.ClassNotFoundException
import io.infinitic.tasks.Task
import io.infinitic.workflows.Workflow
import org.jetbrains.annotations.TestOnly

class TaskExecutorRegisterImpl : WorkerRegister {
    // map task name <> task factory
    private val registeredTasks = mutableMapOf<String, TaskFactory>()
    // map workflow name <> workflow factory
    private val registeredWorkflows = mutableMapOf<String, WorkflowFactory>()

    override fun registerTask(name: String, factory: () -> Task) {
        registeredTasks[name] = factory
    }

    override fun registerWorkflow(name: String, factory: () -> Workflow) {
        registeredWorkflows[name] = factory
    }

    @TestOnly
    override fun unregisterTask(name: String) {
        registeredTasks.remove(name)
    }

    @TestOnly
    override fun unregisterWorkflow(name: String) {
        registeredWorkflows.remove(name)
    }

    override fun getTaskInstance(name: String): Task =
        registeredTasks[name]?.let { it() } ?: throw ClassNotFoundException(name)

    override fun getWorkflowInstance(name: String): Workflow =
        registeredWorkflows[name]?.let { it() } ?: throw ClassNotFoundException(name)
}
