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

package io.infinitic.tests.workflows

import io.infinitic.annotations.Ignore
import io.infinitic.tests.tasks.TaskA
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.Workflow

interface WorkflowB {
    fun concat(input: String): String
    fun factorial(n: Long): Long
    fun cancelChild1(): Long
    fun cancelChild2(): Long
    fun cancelChild2bis(deferred: Deferred<String>): String
}

class WorkflowBImpl : Workflow(), WorkflowB {
    private val taskA = newTask(TaskA::class.java)
    private val workflowB = newWorkflow(WorkflowB::class.java)
    private val workflowA = newWorkflow(WorkflowA::class.java)
    @Ignore
    private val self by lazy { getWorkflowById(WorkflowB::class.java, context.id) }

    override fun concat(input: String): String {
        var str = input

        str = taskA.concat(str, "a")
        str = taskA.concat(str, "b")
        str = taskA.concat(str, "c")

        return str // should be "${input}abc"
    }

    override fun cancelChild1(): Long {
        val def = dispatch(workflowA::channel1)

        taskA.cancelWorkflowA(def.id!!)

        def.await()

        return taskA.await(100)
    }

    override fun cancelChild2(): Long {
        val deferred = dispatch(workflowA::channel1)

        taskA.cancelWorkflowA(deferred.id!!)

        dispatch(self::cancelChild2bis, deferred)

        return taskA.await(200)
    }

    override fun cancelChild2bis(deferred: Deferred<String>): String { return deferred.await() }

    override fun factorial(n: Long) = when {
        n > 1 -> n * workflowB.factorial(n - 1)
        else -> 1
    }
}
