package com.zenaton.taskmanager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskData
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.messages.interfaces.TaskAttemptMessageInterface

data class TaskAttemptDispatched(
    override var taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    override var sentAt: DateTime? = DateTime(),
    val taskName: TaskName,
    val taskData: TaskData?
) : TaskAttemptMessageInterface
