package io.infinitic.common.tasks.messages.interfaces

import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptIndex
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId

interface TaskAttemptMessage {
    val taskId: TaskId
    val taskAttemptId: TaskAttemptId
    val taskAttemptRetry: TaskAttemptRetry
    val taskAttemptIndex: TaskAttemptIndex
}
