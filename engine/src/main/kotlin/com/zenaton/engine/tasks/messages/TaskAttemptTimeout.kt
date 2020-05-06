package com.zenaton.engine.tasks.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.taskAttempts.data.TaskAttemptId
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.tasks.interfaces.TaskAttemptFailingMessageInterface

data class TaskAttemptTimeout(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val taskAttemptDelayBeforeRetry: Float?
) : TaskAttemptFailingMessageInterface
