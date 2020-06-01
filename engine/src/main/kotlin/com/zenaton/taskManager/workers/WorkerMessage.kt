package com.zenaton.taskManager.workers

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.TaskAttemptId
import com.zenaton.taskManager.data.TaskData
import com.zenaton.taskManager.data.TaskId
import com.zenaton.taskManager.data.TaskName
import com.zenaton.taskManager.messages.TaskAttemptMessage

sealed class WorkerMessage {
    abstract val taskName: TaskName
    abstract val sentAt: DateTime
}

data class RunTask(
    override val taskName: TaskName,
    override val sentAt: DateTime = DateTime(),
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskData: TaskData?
) : WorkerMessage(), TaskAttemptMessage
