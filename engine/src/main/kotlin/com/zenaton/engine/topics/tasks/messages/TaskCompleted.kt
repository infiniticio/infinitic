package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput

data class TaskCompleted(
    override var taskId: TaskId,
    override var receivedAt: DateTime? = null,
    val taskOutput: TaskOutput?
) : TaskMessageInterface
