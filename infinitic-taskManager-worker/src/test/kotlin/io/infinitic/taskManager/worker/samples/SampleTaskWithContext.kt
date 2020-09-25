package io.infinitic.taskManager.worker.samples

import io.infinitic.taskManager.worker.TaskAttemptContext

internal class TestingSampleTaskWithContext() {
    private lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String) = (i * j.toInt() * context.taskAttemptIndex.int).toString()
}
