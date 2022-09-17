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

package io.infinitic.tests.deferred

import io.infinitic.annotations.Ignore
import io.infinitic.tests.utils.UtilTask
import io.infinitic.workflows.Deferred
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.and
import io.infinitic.workflows.or
import java.time.Duration

interface DeferredWorkflow {

    fun await(duration: Long): Long
    fun seq1(): String
    fun seq2(): String
    fun seq5(): Long
    fun seq6(): Long
    fun or1(): String
    fun or2(): Any
    fun or3(): String
    fun or4(): String
    fun and1(): List<String>
    fun and2(): List<String>
    fun and3(): List<String>
}

@Suppress("unused")
class DeferredWorkflowImpl : Workflow(), DeferredWorkflow {

    @Ignore
    private val self by lazy { getWorkflowById(DeferredWorkflow::class.java, context.id) }

    lateinit var deferred: Deferred<String>

    private val utilTask = newTask(
        klass = UtilTask::class.java,
        tags = setOf("foo", "bar"),
        meta = mapOf("foo" to "bar".toByteArray())
    )
    private val workflowDeferred = newWorkflow(DeferredWorkflow::class.java)

    private var p1 = ""

    override fun await(duration: Long) = utilTask.await(duration)

    override fun seq1(): String {
        var str = ""
        str = utilTask.concat(str, "1")
        str = utilTask.concat(str, "2")
        str = utilTask.concat(str, "3")

        return str // should be "123"
    }

    override fun seq2(): String {
        var str = ""
        val d = dispatch(utilTask::reverse, "ab")
        str = utilTask.concat(str, "2")
        str = utilTask.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    override fun seq5(): Long {
        var l = 0L
        val d1 = dispatch(utilTask::await, 500)
        val d2 = dispatch(utilTask::await, 100)
        l += d1.await()
        l += d2.await()

        return l // should be 600
    }

    override fun seq6(): Long {
        var l = 0L
        val d1 = dispatch(utilTask::await, 500)
        val d2 = dispatch(utilTask::await, 100)
        l += d1.await()
        l += d2.await()

        // a new step triggers an additional workflowTask
        utilTask.workflowId()

        return l // should be 600
    }

    override fun or1(): String {
        val d1 = dispatch(utilTask::reverse, "ab")
        val d2 = dispatch(utilTask::reverse, "cd")
        val d3 = dispatch(utilTask::reverse, "ef")

        return (d1 or d2 or d3).await() // should be "ba" or "dc" or "fe"
    }

    override fun or2(): Any {
        val d1 = dispatch(utilTask::reverse, "ab")
        val d2 = dispatch(utilTask::reverse, "cd")
        val d3 = dispatch(utilTask::reverse, "ef")

        return ((d1 and d2) or d3).await() // should be listOf("ba","dc") or "fe"
    }

    override fun or3(): String {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(dispatch(utilTask::reverse, "ab"))
        list.add(dispatch(utilTask::reverse, "cd"))
        list.add(dispatch(utilTask::reverse, "ef"))

        return list.or().await() // should be "ba" or "dc" or "fe"
    }

    override fun or4(): String {
        var s3 = utilTask.concat("1", "2")

        val d1 = dispatch(utilTask::reverse, "ab")
        val d2 = timer(Duration.ofMillis(2000))
        val d: Deferred<Any> = (d1 or d2)
        if (d.status() != DeferredStatus.COMPLETED) {
            s3 = utilTask.reverse("ab")
        }

        return (d.await() as String) + s3 // should be "baba"
    }

    override fun and1(): List<String> {
        val d1 = dispatch(utilTask::reverse, "ab")
        val d2 = dispatch(utilTask::reverse, "cd")
        val d3 = dispatch(utilTask::reverse, "ef")

        return (d1 and d2 and d3).await() // should be listOf("ba","dc","fe")
    }

    override fun and2(): List<String> {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(dispatch(utilTask::reverse, "ab"))
        list.add(dispatch(utilTask::reverse, "cd"))
        list.add(dispatch(utilTask::reverse, "ef"))

        return list.and().await() // should be listOf("ba","dc","fe")
    }

    override fun and3(): List<String> {
        val list: MutableList<Deferred<String>> = mutableListOf()
        for (i in 1..20) {
            list.add(dispatch(utilTask::reverse, "ab"))
        }
        return list.and().await() // should be listOf("ba","dc","fe")
    }
}
