package io.infinitic.worker.samples

import io.infinitic.worker.task.TaskAttemptContext

internal class SampleTaskWithTimeout() {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String {
        Thread.sleep(400)

        return (i * j.toInt() * context.taskAttemptIndex.int).toString()
    }
}
