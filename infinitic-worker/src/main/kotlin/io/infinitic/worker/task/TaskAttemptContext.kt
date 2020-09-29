package io.infinitic.worker.task

import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptIndex
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
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
