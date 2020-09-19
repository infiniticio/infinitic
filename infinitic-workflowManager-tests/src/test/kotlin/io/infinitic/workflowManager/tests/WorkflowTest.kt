package io.infinitic.workflowManager.tests

import io.infinitic.workflowManager.worker.Workflow

interface WorkflowA {
    fun test1(): String
}

class WorkflowAImpl : Workflow(), WorkflowA {
    private val task = proxy<TaskTest>()

    override fun test1(): String {
        var str = ""

        str = task.concat(str, "1")
        str = task.concat(str, "2")
        str = task.concat(str, "3")

        return str
    }
}
