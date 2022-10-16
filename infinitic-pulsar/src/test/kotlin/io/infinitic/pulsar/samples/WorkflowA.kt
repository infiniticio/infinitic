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

import io.infinitic.workflows.Deferred
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.and
import io.infinitic.workflows.or
import java.time.LocalDateTime

interface WorkflowA {
    fun empty(): String
    fun seq1(): String
    fun seq2(): String
    fun seq3(): String
    fun seq4(): String
    fun or1(): String
    fun or2(): Any
    fun or3(): String
    fun and1(): List<String>
    fun and2(): List<String>
    fun and3(): List<String>
    fun inline(): String
    fun inline2(): String
    fun inline3(): String
    fun child1(): String
    fun child2(): String
    fun prop1(): String
    fun prop2(): String
    fun prop3(): String
    fun prop4(): String
    fun prop5(): String
    fun prop6(): String
}

@Suppress("unused")
class WorkflowAImpl : Workflow(), WorkflowA {
    private val taskA = newService(TaskA::class.java)
    private val workflowB = newWorkflow(WorkflowB::class.java)
    private var p1 = ""

    override fun empty() = "void"

    override fun seq1(): String {
        var str = ""
        str = taskA.concat(str, "1")
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str // should be "123"
    }

    override fun seq2(): String {
        var str = ""
        val d = dispatch(taskA::reverse, "ab")
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    override fun seq3(): String {
        var str = ""
        val d = dispatch(::seq3bis)
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23ba"
    }

    private fun seq3bis() {
        taskA.reverse("ab")
    }

    override fun seq4(): String {
        var str = ""
        val d = dispatch(::seq4bis)
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.await() // should be "23bac"
    }

    private fun seq4bis() {
        val s = taskA.reverse("ab"); taskA.concat(s, "c")
    }

    override fun or1(): String {
        val d1 = dispatch(taskA::reverse, "ab")
        val d2 = dispatch(taskA::reverse, "cd")
        val d3 = dispatch(taskA::reverse, "ef")

        return (d1 or d2 or d3).await() // should be "ba" or "dc" or "fe"
    }

    override fun or2(): Any {
        val d1 = dispatch(taskA::reverse, "ab")
        val d2 = dispatch(taskA::reverse, "cd")
        val d3 = dispatch(taskA::reverse, "ef")

        return ((d1 and d2) or d3).await() // should be listOf("ba","dc") or "fe"
    }

    override fun or3(): String {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(dispatch(taskA::reverse, "ab"))
        list.add(dispatch(taskA::reverse, "cd"))
        list.add(dispatch(taskA::reverse, "ef"))

        return list.or().await() // should be "ba" or "dc" or "fe"
    }

    override fun and1(): List<String> {
        val d1 = dispatch(taskA::reverse, "ab")
        val d2 = dispatch(taskA::reverse, "cd")
        val d3 = dispatch(taskA::reverse, "ef")

        return (d1 and d2 and d3).await() // should be listOf("ba","dc","fe")
    }

    override fun and2(): List<String> {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(dispatch(taskA::reverse, "ab"))
        list.add(dispatch(taskA::reverse, "cd"))
        list.add(dispatch(taskA::reverse, "ef"))

        return list.and().await() // should be listOf("ba","dc","fe")
    }

    override fun and3(): List<String> {
        val list: MutableList<Deferred<String>> = mutableListOf()
        for (i in 1..1_00) {
            list.add(dispatch(taskA::reverse, "ab"))
        }
        return list.and().await() // should be listOf("ba","dc","fe")
    }

    override fun inline(): String {
        val date = inline { LocalDateTime.now() }
        return taskA.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline2(): String {
        val date = inline {
            dispatch(taskA::reverse, "ab")
            LocalDateTime.now()
        }

        return taskA.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline3(): String {
        val date = inline {
            taskA.concat("1", "2")
            LocalDateTime.now()
        }
        return taskA.concat("Current Date and Time is: ", "$date") // should throw
    }

    override fun child1(): String {
        var str: String = workflowB.concat("-")
        str = taskA.concat(str, "-")

        return str // should be "-abc-"
    }

    override fun child2(): String {
        val str = taskA.reverse("12")
        val d = dispatch(workflowB::concat, str)

        return taskA.concat(d.await(), str) // should be "21abc21"
    }

    override fun prop1(): String {
        p1 = "a"
        dispatch(::prop1bis)
        p1 += "c"

        return p1 // should be "ac"
    }

    private fun prop1bis() {
        p1 += "b"
    }

    override fun prop2(): String {
        p1 = "a"
        dispatch(::prop2Bis)
        p1 += "c"
        taskA.await(100)
        p1 += "d"

        return p1 // should be "acbd"
    }

    fun prop2Bis() {
        p1 += "b"
    }

    override fun prop3(): String {
        p1 = "a"
        dispatch(::prop3bis)
        p1 += "c"
        taskA.await(100)
        p1 += "d"

        return p1 // should be "acbd"
    }

    fun prop3bis() {
        taskA.await(50); p1 += "b"
    }

    override fun prop4(): String {
        p1 = "a"
        dispatch(::prop4bis)
        p1 += "c"
        taskA.await(100)
        p1 += "d"

        return p1 // should be "acd"
    }

    private fun prop4bis() {
        taskA.await(150); p1 += "b"
    }

    override fun prop5(): String {
        p1 = "a"
        dispatch(::prop5bis)
        dispatch(::prop5ter)
        p1 += "d"
        taskA.await(100)

        return p1 // should be "adbc"
    }

    private fun prop5bis() {
        p1 += "b"
    }

    private fun prop5ter() {
        p1 += "c"
    }

    override fun prop6(): String {
        val d1 = dispatch(taskA::reverse, "12")
        val d2 = dispatch(::prop6bis, d1)
        d1.await()
        p1 += "a"
        p1 = d2.await() + p1
        // unfortunately p1 = p1 + d2.await() would fail the test
        // because d2.await() updates p1 value too lately in the expression
        // not sure, how to avoid that

        return p1 // should be "abab"
    }

    private fun prop6bis(d1: Deferred<*>): String {
        d1.await(); p1 += "b"; return p1
    }
}
