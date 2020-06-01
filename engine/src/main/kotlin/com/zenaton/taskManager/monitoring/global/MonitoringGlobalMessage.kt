package com.zenaton.taskManager.monitoring.global

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.TaskName

sealed class MonitoringGlobalMessage {
    abstract val sentAt: DateTime
    abstract val taskName: TaskName
}

data class TaskCreated(
    override val sentAt: DateTime = DateTime(),
    override val taskName: TaskName
) : MonitoringGlobalMessage()
