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

package io.infinitic.workflows.tests.workflows

import io.infinitic.workflows.Workflow
import io.infinitic.workflows.tests.tasks.TaskA

interface WorkflowB {
    fun concat(input: String): String
    fun factorial(n: Long): Long
    fun cancelChild(): String
}

class WorkflowBImpl : Workflow(), WorkflowB {
    private val task = newTask<TaskA>()
    private val workflow = newWorkflow<WorkflowB>()
    private val workflowA = newWorkflow<WorkflowA>()

    override fun concat(input: String): String {
        var str = input

        str = task.concat(str, "a")
        str = task.concat(str, "b")
        str = task.concat(str, "c")

        return str // should be "${input}abc"
    }

    override fun cancelChild(): String {
        val def = async(workflowA) { channel1() }
        task.cancelWorkflowA(def.id!!)

        return def.await()
    }

    override fun factorial(n: Long) = when {
        n > 1 -> n * workflow.factorial(n - 1)
        else -> 1
    }
}
