package com.zenaton.engine.taskAttempts.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.taskAttempts.data.TaskAttemptId
import com.zenaton.engine.tasks.data.TaskData
import com.zenaton.engine.tasks.data.TaskId
import com.zenaton.engine.tasks.data.TaskName

data class TaskAttemptDispatched(
    override var taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override var sentAt: DateTime? = DateTime(),
    override var receivedAt: DateTime? = null,
    val taskName: TaskName,
    val taskData: TaskData?
) : TaskAttemptInterface
