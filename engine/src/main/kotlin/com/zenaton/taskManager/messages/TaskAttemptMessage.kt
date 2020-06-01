package com.zenaton.taskManager.messages

import com.zenaton.taskManager.data.TaskAttemptId

interface TaskAttemptMessage : TaskMessage {
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
}
