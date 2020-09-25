package io.infinitic.taskManager.worker.samples

import io.infinitic.taskManager.worker.TaskAttemptContext

internal class SampleTaskWithRetry() {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    fun getRetryDelay(): Float? = if (context.exception is IllegalStateException) 3F else 0F
}

internal class SampleTaskWithBadTypeRetry() {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    fun getRetryDelay(): Int? = 3
}

internal class SampleTaskWithBuggyRetry() {
    lateinit var context: TaskAttemptContext

    fun handle(i: Int, j: String): String = if (i < 0) (i * j.toInt()).toString() else throw IllegalStateException()

    fun getRetryDelay(): Float? = if (context.exception is IllegalStateException) throw IllegalArgumentException() else 3F
}
