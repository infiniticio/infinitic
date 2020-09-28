package io.infinitic.tests.workflows.samples

import io.infinitic.worker.workflowTask.Workflow

interface WorkflowB : io.infinitic.common.workflowManager.Workflow {
    fun concat(input: String): String
    fun factorial(n: Long): Long
}

class WorkflowBImpl : Workflow(), WorkflowB {
    private val task = proxy(TaskA::class.java)
    private val workflow = proxy(WorkflowB::class.java)

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
