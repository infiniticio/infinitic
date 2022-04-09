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

package io.infinitic.tests.branches

import io.infinitic.annotations.Ignore
import io.infinitic.tests.utils.UtilTask
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.Workflow

interface BranchesWorkflow {
    fun seq3(): String
    fun seq3bis(): String
    fun seq4(): String
    fun seq4bis(): String
    fun deferred1(): String
    fun deferred1bis(): String
}

@Suppress("unused")
class BranchesWorkflowImpl : Workflow(), BranchesWorkflow {

    @Ignore private val self by lazy { getWorkflowById(BranchesWorkflow::class.java, context.id) }

    lateinit var deferred: Deferred<String>

    private val utilTask = newTask(UtilTask::class.java, tags = setOf("foo", "bar"), meta = mapOf("foo" to "bar".toByteArray()))
    private val branchesWorkflow = newWorkflow(BranchesWorkflow::class.java)

    override fun seq3(): String {
        var str = ""
        val d = dispatch(self::seq3bis)
        str = utilTask.concat(str, "2")
        str = utilTask.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    override fun seq3bis(): String { return utilTask.reverse("ab") }

    override fun seq4(): String {
        var str = ""
        val d = dispatch(self::seq4bis)
        str = utilTask.concat(str, "2")
        str = utilTask.concat(str, "3")

        return str + d.await() // should be "23bac"
    }

    override fun seq4bis(): String {
        val s = utilTask.reverse("ab")
        return utilTask.concat(s, "c")
    }

    override fun deferred1(): String {
        var str = ""
        val d = dispatch(self::deferred1bis)
        str += d.isOngoing().toString()
        str += d.isCompleted().toString()
        d.await()
        str += d.isOngoing().toString()
        str += d.isCompleted().toString()

        return str // should be "truefalsefalsetrue"
    }

    override fun deferred1bis(): String { return utilTask.reverse("X") }
}
