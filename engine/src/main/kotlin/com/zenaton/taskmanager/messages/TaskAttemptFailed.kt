package com.zenaton.taskmanager.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptError
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptFailingMessageInterface

data class TaskAttemptFailed(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val taskAttemptDelayBeforeRetry: Float,
    val taskAttemptError: TaskAttemptError
) : TaskAttemptFailingMessageInterface
