package io.infinitic.workflowManager.worker

interface WorkflowA {
    fun test1(): String
}

interface TaskA {
    fun log()
}

class WorkflowAImpl : Workflow(), WorkflowA {
    private val task = taskProxy<TaskA>()

    override fun test1(): String {
        task.log()
        task.log()
        task.log()

        return "ok"
    }
}

class TaskAImpl {
    companion object {
        var log = ""
    }

    fun log() {
        log += "A"
    }
}
