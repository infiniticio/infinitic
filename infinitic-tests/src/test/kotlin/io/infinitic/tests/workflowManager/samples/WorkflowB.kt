package io.infinitic.tests.workflowManager.samples

import io.infinitic.worker.workflowManager.Workflow

interface WorkflowB {
    fun concat(input: String): String
    fun factorial(n: Long): Long
}

class WorkflowBImpl : Workflow(), WorkflowB {
    private val task = proxy<TaskA>()
    private val workflow = proxy<WorkflowB>()

    override fun concat(input: String): String {
        var str = input

        str = task.concat(str, "a")
        str = task.concat(str, "b")
        str = task.concat(str, "c")

        return str // should be "${input}123"
    }

    override fun factorial(n: Long): Long {
        return if (n > 1) {
            n * workflow.factorial(n - 1)
        } else {
            1
        }
    }
}
