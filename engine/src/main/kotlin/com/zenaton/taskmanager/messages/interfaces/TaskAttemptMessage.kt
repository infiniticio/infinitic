package com.zenaton.taskmanager.messages.interfaces

import com.zenaton.taskmanager.data.TaskAttemptId

interface TaskAttemptMessage : TaskMessage {
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
}
