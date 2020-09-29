package io.infinitic.worker.samples

import io.infinitic.common.tasks.Task
import io.infinitic.worker.task.TaskAttemptContext

internal class SampleTaskWithRetry() : Task {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    fun getRetryDelay(): Float? = if (context.exception is IllegalStateException) 3F else 0F
}

internal class SampleTaskWithBadTypeRetry() : Task {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    fun getRetryDelay(): Int? = 3
}

internal class SampleTaskWithBuggyRetry() : Task {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    fun getRetryDelay(): Float? = if (context.exception is IllegalStateException) throw IllegalArgumentException() else 3F
}
