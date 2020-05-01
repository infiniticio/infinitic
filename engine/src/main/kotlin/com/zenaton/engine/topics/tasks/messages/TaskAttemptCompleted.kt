package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskAttemptId
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskOutput

class TaskAttemptCompleted(
    override var taskId: TaskId,
    override var receivedAt: DateTime? = null,
    val taskAttemptId: TaskAttemptId,
    val taskOutput: TaskOutput?
) : TaskMessageInterface
