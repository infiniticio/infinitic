package io.infinitic.workflowManager.tests.samples

import io.infinitic.workflowManager.worker.Workflow
import io.infinitic.workflowManager.worker.deferred.Deferred
import io.infinitic.workflowManager.worker.deferred.and
import io.infinitic.workflowManager.worker.deferred.or
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
}

class WorkflowAImpl : Workflow(), WorkflowA {
    private val task = proxy<TaskA>()

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
}
