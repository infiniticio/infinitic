package io.infinitic.worker.samples

import io.infinitic.common.taskManager.Task
import io.infinitic.worker.task.TaskAttemptContext

internal class SampleTaskWithContext() : Task {
    private lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String) = (i * j.toInt() * context.taskAttemptIndex.int).toString()
}
