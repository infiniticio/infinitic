package io.infinitic.taskManager.common.data

data class TaskAttemptContext(
    val taskId: TaskId,
    val taskAttemptId: TaskAttemptId,
    val taskAttemptIndex: TaskAttemptIndex,
    val taskAttemptRetry: TaskAttemptRetry,
    var exception: Throwable? = null,
    val taskMeta: TaskMeta,
    val taskOptions: TaskOptions
)
