package com.zenaton.taskmanager.messages.metrics

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface

data class TaskStatusUpdated(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    val taskName: TaskName,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus?
) : TaskMessageInterface
