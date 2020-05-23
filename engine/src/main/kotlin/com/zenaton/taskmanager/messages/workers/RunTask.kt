package com.zenaton.taskmanager.messages.workers

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessageInterface

data class RunTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskName: TaskName,
    val taskData: TaskData?
) : TaskAttemptMessageInterface
