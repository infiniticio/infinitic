package my.infinitic.project.workflows

import io.infinitic.common.workflows.executors.Workflow
import io.infinitic.common.workflows.executors.WorkflowTaskContext
import io.infinitic.common.workflows.executors.proxy
import my.infinitic.project.tasks.Add
import my.infinitic.project.tasks.Multiply

interface Add10AndMultiplyBy2 : Workflow {
    fun handle(n: Int): Int
}

class Add10AndMultiplyBy2Impl : Add10AndMultiplyBy2 {
    override lateinit var context: WorkflowTaskContext
    private val addTask = proxy(Add::class)
    private val multiplyTask = proxy(Multiply::class)

    override fun handle(n: Int): Int {
        val nPlus10 = addTask.add(n, 10)

        return multiplyTask.multiply(nPlus10, 2)
    }
}
