// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.tests.workflows.samples

import io.infinitic.common.workflows.Deferred
import io.infinitic.common.workflows.WorkflowTaskContext
import io.infinitic.common.workflows.and
import io.infinitic.common.workflows.or
import io.infinitic.common.workflows.Workflow
import io.infinitic.common.workflows.async
import io.infinitic.common.workflows.proxy
import io.infinitic.common.workflows.task
import java.time.LocalDateTime

interface WorkflowA : Workflow {
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
}

class WorkflowAImpl() : WorkflowA {
    override lateinit var context: WorkflowTaskContext

    private val taskA = proxy(TaskA::class.java)
    private val workflowB = proxy(WorkflowB::class.java)

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

        val d = async(taskA) { reverse("ab") }
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.result() // should be "23ba"
    }

    override fun seq3(): String {
        var str = ""

        val d = async { taskA.reverse("ab") }
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.result() // should be "23ba"
    }

    override fun seq4(): String {
        var str = ""

        val d = async {
            val s = taskA.reverse("ab")
            taskA.concat(s, "c")
        }
        str = taskA.concat(str, "2")
        str = taskA.concat(str, "3")

        return str + d.result() // should be "23bac"
    }

    override fun or1(): String {
        val d1 = async(taskA) { reverse("ab") }
        val d2 = async(taskA) { reverse("cd") }
        val d3 = async(taskA) { reverse("ef") }

        return (d1 or d2 or d3).result() // should be "ba" or "dc" or "fe"
    }

    override fun or2(): Any {
        val d1 = async(taskA) { reverse("ab") }
        val d2 = async(taskA) { reverse("cd") }
        val d3 = async(taskA) { reverse("ef") }

        return ((d1 and d2) or d3).result() // should be listOf("ba","dc") or "fe"
    }

    override fun or3(): String {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(async(taskA) { reverse("ab") })
        list.add(async(taskA) { reverse("cd") })
        list.add(async(taskA) { reverse("ef") })

        return list.or().result() // should be "ba" or "dc" or "fe"
    }

    override fun and1(): List<String> {
        val d1 = async(taskA) { reverse("ab") }
        val d2 = async(taskA) { reverse("cd") }
        val d3 = async(taskA) { reverse("ef") }

        return (d1 and d2 and d3).result() // should be listOf("ba","dc","fe")
    }

    override fun and2(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(async(taskA) { reverse("ab") })
        list.add(async(taskA) { reverse("cd") })
        list.add(async(taskA) { reverse("ef") })

        return list.and().result() // should be listOf("ba","dc","fe")
    }

    override fun and3(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        for (i in 1..1_000) {
            list.add(async(taskA) { reverse("ab") })
        }
        return list.and().result() // should be listOf("ba","dc","fe")
    }

    override fun inline(): String {
        val date = task { LocalDateTime.now() }
        return taskA.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline2(): String {
        val date = task {
            async(taskA) { reverse("ab") }
            LocalDateTime.now()
        }

        return taskA.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline3(): String {
        val date = task {
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
        val d = async(workflowB) { concat(str) }

        return taskA.concat(d.result(), str) // should be "21abc21"
    }
}
