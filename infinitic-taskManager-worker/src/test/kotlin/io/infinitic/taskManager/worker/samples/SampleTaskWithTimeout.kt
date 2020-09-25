package io.infinitic.taskManager.worker.samples

import io.infinitic.taskManager.worker.TaskAttemptContext

internal class TestingSampleTaskWithTimeout() {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String {
        Thread.sleep(400)

        return (i * j.toInt() * context.taskAttemptIndex.int).toString()
    }
}
