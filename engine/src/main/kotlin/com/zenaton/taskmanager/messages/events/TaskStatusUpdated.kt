package com.zenaton.taskmanager.messages.events

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.TaskMessageInterface

data class TaskStatusUpdated(
    override var taskId: TaskId,
    override var sentAt: DateTime = DateTime(),
    var taskName: TaskName,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus?
) : TaskMessageInterface
