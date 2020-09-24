package io.infinitic.workflowManager.tests.samples

import io.infinitic.workflowManager.worker.Workflow

interface WorkflowB {
    fun concat(input: String): String
}

class WorkflowBImpl : Workflow(), WorkflowB {
    private val task = proxy<TaskA>()

    override fun concat(input: String): String {
        var str = input

        str = task.concat(str, "a")
        str = task.concat(str, "b")
        str = task.concat(str, "c")

        return str // should be "${input}123"
    }
}
