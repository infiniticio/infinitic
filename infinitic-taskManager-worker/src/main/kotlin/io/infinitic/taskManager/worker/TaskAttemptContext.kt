package io.infinitic.taskManager.worker

import io.infinitic.taskManager.common.data.TaskAttemptId
import io.infinitic.taskManager.common.data.TaskAttemptIndex
import io.infinitic.taskManager.common.data.TaskAttemptRetry
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskMeta
import io.infinitic.taskManager.common.data.TaskOptions

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
