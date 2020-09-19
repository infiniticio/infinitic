package io.infinitic.workflowManager.tests

import io.infinitic.workflowManager.worker.Workflow

interface WorkflowA {
    fun empty(): String
    fun seq1(): String
    fun seq2(): String
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
}
