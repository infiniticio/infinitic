package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskAttemptId
import com.zenaton.engine.data.tasks.TaskId

data class TaskAttemptStarted(
    override var taskId: TaskId,
    override var receivedAt: DateTime? = null,
    val taskAttemptId: TaskAttemptId
) : TaskMessageInterface
