package io.infinitic.tests.workflows.samples

import io.infinitic.worker.workflowTask.Workflow
import io.infinitic.worker.workflowTask.deferred.Deferred
import io.infinitic.worker.workflowTask.deferred.and
import io.infinitic.worker.workflowTask.deferred.or
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
}

class WorkflowAImpl : Workflow(), WorkflowA {
    private val task = proxy<TaskA>()
    private val workflowB = proxy<WorkflowB>()

    override fun empty() = "void"

    override fun seq1(): String {
        var str = ""

        str = task.concat(str, "1")
        str = task.concat(str, "2")
        str = task.concat(str, "3")

        return str // should be "123"
    }

    override fun seq2(): String {
        var str = ""

        val d = async(task) { reverse("ab") }
        str = task.concat(str, "2")
        str = task.concat(str, "3")

        return str + d.result() // should be "23ba"
    }

    override fun seq3(): String {
        var str = ""

        val d = async { task.reverse("ab") }
        str = task.concat(str, "2")
        str = task.concat(str, "3")

        return str + d.result() // should be "23ba"
    }

    override fun seq4(): String {
        var str = ""

        val d = async {
            val s = task.reverse("ab")
            task.concat(s, "c")
        }
        str = task.concat(str, "2")
        str = task.concat(str, "3")

        return str + d.result() // should be "23bac"
    }

    override fun or1(): String {
        val d1 = async(task) { reverse("ab") }
        val d2 = async(task) { reverse("cd") }
        val d3 = async(task) { reverse("ef") }

        return (d1 or d2 or d3).result() // should be "ba" or "dc" or "fe"
    }

    override fun or2(): Any {
        val d1 = async(task) { reverse("ab") }
        val d2 = async(task) { reverse("cd") }
        val d3 = async(task) { reverse("ef") }

        return ((d1 and d2) or d3).result() // should be listOf("ba","dc") or "fe"
    }

    override fun or3(): String {
        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(async(task) { reverse("ab") })
        list.add(async(task) { reverse("cd") })
        list.add(async(task) { reverse("ef") })

        return list.or().result() // should be "ba" or "dc" or "fe"
    }

    override fun and1(): List<String> {
        val d1 = async(task) { reverse("ab") }
        val d2 = async(task) { reverse("cd") }
        val d3 = async(task) { reverse("ef") }

        return (d1 and d2 and d3).result() // should be listOf("ba","dc","fe")
    }

    override fun and2(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        list.add(async(task) { reverse("ab") })
        list.add(async(task) { reverse("cd") })
        list.add(async(task) { reverse("ef") })

        return list.and().result() // should be listOf("ba","dc","fe")
    }

    override fun and3(): List<String> {

        val list: MutableList<Deferred<String>> = mutableListOf()
        for (i in 1..1_000) {
            list.add(async(task) { reverse("ab") })
        }
        return list.and().result() // should be listOf("ba","dc","fe")
    }

    override fun inline(): String {
        val date = task { LocalDateTime.now() }
        return task.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline2(): String {
        val date = task {
            async(task) { reverse("ab") }
            LocalDateTime.now()
        }

        return task.concat("Current Date and Time is: ", "$date") // should not throw
    }

    override fun inline3(): String {
        val date = task {
            task.concat("1", "2")
            LocalDateTime.now()
        }
        return task.concat("Current Date and Time is: ", "$date") // should throw
    }

    override fun child1(): String {

        var str: String = workflowB.concat("-")
        str = task.concat(str, "-")

        return str // should be "-abc-"
    }

    override fun child2(): String {
        val str = task.reverse("12")
        val d = async(workflowB) { concat(str) }

        return task.concat(d.result(), str) // should be "21abc21"
    }
}
