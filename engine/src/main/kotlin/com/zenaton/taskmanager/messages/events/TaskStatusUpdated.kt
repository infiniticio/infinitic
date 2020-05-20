package com.zenaton.taskmanager.messages.events

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.TaskMessageInterface

data class TaskStatusUpdated(
    override var taskId: TaskId,
    var taskName: TaskName,
    override var sentAt: DateTime = DateTime(),
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus?
) : TaskMessageInterface
