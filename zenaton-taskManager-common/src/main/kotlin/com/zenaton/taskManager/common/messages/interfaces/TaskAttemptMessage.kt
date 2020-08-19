package com.zenaton.taskManager.common.messages.interfaces

import com.zenaton.taskManager.common.data.TaskAttemptId
import com.zenaton.taskManager.common.data.TaskAttemptIndex
import com.zenaton.taskManager.common.data.TaskAttemptRetry
import com.zenaton.taskManager.common.data.TaskId

interface TaskAttemptMessage {
    val taskId: TaskId
    val taskAttemptId: TaskAttemptId
    val taskAttemptRetry: TaskAttemptRetry
    val taskAttemptIndex: TaskAttemptIndex
}
