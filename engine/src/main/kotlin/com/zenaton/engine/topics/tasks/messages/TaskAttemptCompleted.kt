package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.TaskAttemptId
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.TaskOutput
import com.zenaton.engine.topics.tasks.interfaces.TaskMessageInterface

data class TaskAttemptCompleted(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime(),
    val taskAttemptId: TaskAttemptId,
    val taskAttemptIndex: Int,
    val taskOutput: TaskOutput?
) : TaskMessageInterface
