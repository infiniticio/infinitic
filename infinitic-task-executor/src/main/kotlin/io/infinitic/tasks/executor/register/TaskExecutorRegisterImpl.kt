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

import io.infinitic.common.tasks.exceptions.ClassNotFoundDuringInstantiation
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.exceptions.TaskUsedAsWorkflow
import io.infinitic.common.workflows.exceptions.WorkflowUsedAsTask
import io.infinitic.tasks.executor.workflowTask.WorkflowTaskImpl
import io.infinitic.workflows.AbstractWorkflow
import io.infinitic.workflows.Workflow

class TaskExecutorRegisterImpl : TaskExecutorRegister {
    // map taskName <> task factory
    private val registeredFactories = mutableMapOf<String, InstanceFactory>()

    // WorkflowTask auto register
    init {
        register(WorkflowTask::class.java.name) { WorkflowTaskImpl() }
    }

    override fun register(name: String, factory: () -> Any) {
        registeredFactories[name] = factory
    }

    override fun unregister(name: String) {
        registeredFactories.remove(name)
    }

    override fun getTaskInstance(name: String): Any {
        val instance = getInstance(name)
        if (instance is Workflow) throw WorkflowUsedAsTask(name, instance::class.qualifiedName!!)
        else return instance
    }

    override fun getWorkflowInstance(name: String): AbstractWorkflow {
        val instance = getInstance(name)
        if (instance !is AbstractWorkflow) throw TaskUsedAsWorkflow(name, instance::class.qualifiedName!!)
        else return instance
    }

    override fun getTasks() =
        registeredFactories
            .map { (name, factory) -> name to factory() }
            .filterNot { (_, instance) -> instance is Workflow }
            .map { (name, _) -> name }

    private fun getInstance(name: String) =
        registeredFactories[name]?.let { it() } ?: throw ClassNotFoundDuringInstantiation(name)

    @PublishedApi
    internal val `access$registeredFactories`: MutableMap<String, InstanceFactory>
        get() = registeredFactories
}
