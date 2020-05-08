package com.zenaton.engine.topics.taskAttempts.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.TaskAttemptId
import com.zenaton.engine.data.TaskData
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.TaskName
import com.zenaton.engine.topics.taskAttempts.interfaces.TaskAttemptMessageInterface

data class TaskAttemptDispatched(
    override var taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override var sentAt: DateTime? = DateTime(),
    val taskName: TaskName,
    val taskData: TaskData?
) : TaskAttemptMessageInterface {
    override fun getName() = taskName
}
