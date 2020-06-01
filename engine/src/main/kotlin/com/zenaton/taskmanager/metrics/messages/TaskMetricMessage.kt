package com.zenaton.taskmanager.metrics.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus

sealed class TaskMetricMessage {
    abstract val sentAt: DateTime
    abstract val taskName: TaskName
}

data class TaskStatusUpdated constructor(
    override val sentAt: DateTime = DateTime(),
    override val taskName: TaskName,
    val taskId: TaskId,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus
) : TaskMetricMessage()
