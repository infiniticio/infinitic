package io.infinitic.worker.task

import io.infinitic.common.taskManager.data.TaskAttemptId
import io.infinitic.common.taskManager.data.TaskAttemptIndex
import io.infinitic.common.taskManager.data.TaskAttemptRetry
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.taskManager.data.TaskOptions
import io.infinitic.worker.Worker

data class TaskAttemptContext(
    val worker: Worker,
    val taskId: TaskId,
    val taskAttemptId: TaskAttemptId,
    val taskAttemptIndex: TaskAttemptIndex,
    val taskAttemptRetry: TaskAttemptRetry,
    var exception: Throwable? = null,
    val taskMeta: TaskMeta,
    val taskOptions: TaskOptions
)
