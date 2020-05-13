package com.zenaton.taskmanager.messages.interfaces

import com.zenaton.taskmanager.data.TaskAttemptId

interface TaskAttemptMessageInterface : TaskMessageInterface {
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
}
