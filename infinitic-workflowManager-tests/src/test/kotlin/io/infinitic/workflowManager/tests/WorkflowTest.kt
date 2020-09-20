package io.infinitic.workflowManager.tests

import io.infinitic.workflowManager.worker.Workflow
import io.infinitic.workflowManager.worker.deferred.and
import io.infinitic.workflowManager.worker.deferred.or

interface WorkflowA {
    fun empty(): String
    fun seq1(): String
    fun seq2(): String
    fun seq3(): String
    fun seq4(): String
    fun or1(): String
    fun or2(): Any
    fun and1(): List<String>
}

class WorkflowAImpl : Workflow(), WorkflowA {
    private val task = proxy<TaskTest>()

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

        return (d1 or d2 or d3).result() // should be "ba" TODO: make this test deterministic
    }

    override fun or2(): Any {
        val d1 = async(task) { reverse("ab") }
        val d2 = async(task) { reverse("cd") }
        val d3 = async(task) { reverse("ef") }

        return ((d1 and d2) or d3).result() // should be listOf("ba","dc") TODO: make this test deterministic
    }

    override fun and1(): List<String> {
        val d1 = async(task) { reverse("ab") }
        val d2 = async(task) { reverse("cd") }
        val d3 = async(task) { reverse("ef") }

        return (d1 and d2 and d3).result() // should be listOf("ba","dc","fe")
    }
}
