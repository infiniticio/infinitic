package io.infinitic.worker.taskManager.samples

import io.infinitic.worker.taskManager.TaskAttemptContext

internal class SampleTaskWithContext() {
    private lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String) = (i * j.toInt() * context.taskAttemptIndex.int).toString()
}
