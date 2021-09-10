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

package io.infinitic.tasks

import io.infinitic.workflows.Workflow

typealias TaskFactory = () -> Task

typealias WorkflowFactory = () -> Workflow

interface TaskExecutorRegister {

    /**
     * Register a task factory
     */
    fun registerTask(name: String, factory: TaskFactory)

    /**
     * Register a workflow factory
     */
    fun registerWorkflow(name: String, factory: WorkflowFactory)

    /**
     * Unregister a given name (mostly used in tests)
     */
    fun unregisterTask(name: String)

    /**
     * Unregister a given name (mostly used in tests)
     */
    fun unregisterWorkflow(name: String)

    /**
     * Get task instance per name
     */
    fun getTaskInstance(name: String): Task

    /**
     * Get workflow instance per name
     */
    fun getWorkflowInstance(name: String): Workflow
}

inline fun <reified T> TaskExecutorRegister.registerTask(noinline factory: TaskFactory) =
    this.registerTask(T::class.java.name, factory)

inline fun <reified T> TaskExecutorRegister.registerWorkflow(noinline factory: WorkflowFactory) =
    this.registerWorkflow(T::class.java.name, factory)
