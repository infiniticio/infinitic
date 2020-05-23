package com.zenaton.taskmanager.messages.interfaces

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskId

interface FailingTaskAttemptMessageInterface : TaskAttemptMessageInterface {
    override val taskId: TaskId
    override val sentAt: DateTime
    override val taskAttemptId: TaskAttemptId
    override val taskAttemptIndex: Int
    val taskAttemptDelayBeforeRetry: Float?
}
