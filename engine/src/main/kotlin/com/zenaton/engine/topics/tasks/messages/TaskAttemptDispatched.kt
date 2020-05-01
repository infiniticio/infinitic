package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskAttemptId
import com.zenaton.engine.data.tasks.TaskData
import com.zenaton.engine.data.tasks.TaskId
import com.zenaton.engine.data.tasks.TaskName

data class TaskAttemptDispatched(
    override var taskId: TaskId,
    override var receivedAt: DateTime? = null,
    val taskAttemptId: TaskAttemptId,
    val taskName: TaskName?,
    val taskData: TaskData?
) : TaskMessageInterface
