package com.zenaton.taskmanager.messages.interfaces

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskId

interface TaskAttemptMessageInterface : TaskMessageInterface {
    override val taskId: TaskId
    override val sentAt: DateTime
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
}
