package com.zenaton.taskmanager.messages

import com.zenaton.taskmanager.data.TaskAttemptId

interface TaskAttemptMessageInterface : TaskMessageInterface {
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
}
