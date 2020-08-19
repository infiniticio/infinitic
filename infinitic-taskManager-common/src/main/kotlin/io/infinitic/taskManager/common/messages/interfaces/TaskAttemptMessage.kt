package io.infinitic.taskManager.common.messages.interfaces

import io.infinitic.taskManager.common.data.TaskAttemptId
import io.infinitic.taskManager.common.data.TaskAttemptIndex
import io.infinitic.taskManager.common.data.TaskAttemptRetry
import io.infinitic.taskManager.common.data.TaskId

interface TaskAttemptMessage {
    val taskId: TaskId
    val taskAttemptId: TaskAttemptId
    val taskAttemptRetry: TaskAttemptRetry
    val taskAttemptIndex: TaskAttemptIndex
}
