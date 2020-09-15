package io.infinitic.workflowManager.tests

import io.infinitic.workflowManager.worker.Workflow

interface WorkflowA {
    fun test1(): String
}

class WorkflowAImpl : Workflow(), WorkflowA {
    private val task = proxy<TaskTest>()

    override fun test1(): String {
        task.log()
        task.log()

        return "ok"
    }
}
