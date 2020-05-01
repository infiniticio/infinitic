package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskAttemptError
import com.zenaton.engine.data.tasks.TaskAttemptId
import com.zenaton.engine.data.tasks.TaskId

data class TaskAttemptFailed(
    override var taskId: TaskId,
    override var receivedAt: DateTime? = null,
    val taskAttemptId: TaskAttemptId,
    val taskAttemptError: TaskAttemptError
) : TaskMessageInterface
