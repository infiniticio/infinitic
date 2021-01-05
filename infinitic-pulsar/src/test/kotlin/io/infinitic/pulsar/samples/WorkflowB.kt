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

package io.infinitic.pulsar.samples

import io.infinitic.workflows.Workflow
import io.infinitic.workflows.WorkflowTaskContext
import io.infinitic.workflows.task
import io.infinitic.workflows.workflow

interface WorkflowB : Workflow {
    fun concat(input: String): String
    fun factorial(n: Long): Long
}

class WorkflowBImpl() : WorkflowB {
    override lateinit var context: WorkflowTaskContext

    private val task = task<TaskA>()
    private val workflow = workflow<WorkflowB>()

    override fun concat(input: String): String {
        var str = input

        str = task.concat(str, "a")
        str = task.concat(str, "b")
        str = task.concat(str, "c")

        return str // should be "${input}123"
    }

    override fun factorial(n: Long): Long {
        return if (n > 1) {
            n * workflow.factorial(n - 1)
        } else {
            1
        }
    }
}
