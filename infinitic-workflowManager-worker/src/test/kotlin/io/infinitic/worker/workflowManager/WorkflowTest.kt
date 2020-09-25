package io.infinitic.worker.workflowManager

interface WorkflowA {
    fun test1(i: Int): String
}

interface TaskA {
//    @Throws(Throwable::class)
    fun log(i: Int) = i + 1
}

class WorkflowAImpl : Workflow(), WorkflowA {
    private val task = proxy<TaskA>()

    override fun test1(i: Int): String {
        var j = ""
//        var j = task.log(i)
//        j += task.log(j)
//        j += task.log(j)
//
        val s = async(task) { log(i) }
        task.log(i)

        return j.toString()
    }
}

class TaskAImpl : TaskA {
    companion object {
        var log = ""
    }
}
