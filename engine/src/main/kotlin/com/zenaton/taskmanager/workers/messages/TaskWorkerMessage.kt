package com.zenaton.taskmanager.workers.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.messages.TaskAttemptMessage

sealed class TaskWorkerMessage {
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
) : TaskWorkerMessage(), TaskAttemptMessage
