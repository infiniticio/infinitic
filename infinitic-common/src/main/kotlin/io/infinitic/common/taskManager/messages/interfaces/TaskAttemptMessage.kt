package io.infinitic.common.taskManager.messages.interfaces

import io.infinitic.common.taskManager.data.TaskAttemptId
import io.infinitic.common.taskManager.data.TaskAttemptIndex
import io.infinitic.common.taskManager.data.TaskAttemptRetry
import io.infinitic.common.taskManager.data.TaskId

interface TaskAttemptMessage {
    val taskId: TaskId
    val taskAttemptId: TaskAttemptId
    val taskAttemptRetry: TaskAttemptRetry
    val taskAttemptIndex: TaskAttemptIndex
}
