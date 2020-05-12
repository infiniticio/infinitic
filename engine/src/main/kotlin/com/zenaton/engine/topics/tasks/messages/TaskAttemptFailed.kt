package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.TaskAttemptError
import com.zenaton.engine.data.TaskAttemptId
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.topics.tasks.interfaces.TaskAttemptFailingMessageInterface

data class TaskAttemptFailed(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override val taskAttemptDelayBeforeRetry: Float,
    val taskAttemptError: TaskAttemptError
) : TaskAttemptFailingMessageInterface
