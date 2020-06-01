package com.zenaton.taskManager.monitoring.perName

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.TaskId
import com.zenaton.taskManager.data.TaskName
import com.zenaton.taskManager.data.TaskStatus

sealed class MonitoringPerNameMessage {
    abstract val sentAt: DateTime
    abstract val taskName: TaskName
}

data class TaskStatusUpdated constructor(
    override val sentAt: DateTime = DateTime(),
    override val taskName: TaskName,
    val taskId: TaskId,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus
) : MonitoringPerNameMessage()
