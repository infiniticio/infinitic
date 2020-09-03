package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.worker.data.WorkflowTaskContext
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {
    val w = WorkflowAImpl()
    w.workflowTaskContext = WorkflowTaskContext(
        blockingSteps = listOf(),
        commands = listOf()
    )
    w.test1()
}
