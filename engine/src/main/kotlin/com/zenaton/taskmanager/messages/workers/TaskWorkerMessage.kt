package com.zenaton.taskmanager.messages.workers

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessageInterface

sealed class TaskWorkerMessage(
    open val taskName: TaskName,
    open val sentAt: DateTime
)

data class RunTask(
    override val taskName: TaskName,
    override val sentAt: DateTime = DateTime(),
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskData: TaskData?
) : TaskWorkerMessage(taskName, sentAt), TaskAttemptMessageInterface
