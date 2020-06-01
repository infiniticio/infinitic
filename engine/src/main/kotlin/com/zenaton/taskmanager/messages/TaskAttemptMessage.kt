package com.zenaton.taskmanager.messages

import com.zenaton.taskmanager.data.TaskAttemptId

interface TaskAttemptMessage : TaskMessage {
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
}
