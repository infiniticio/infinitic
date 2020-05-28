package com.zenaton.taskmanager.messages.metrics

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus

sealed class TaskMetricMessage {
    abstract val sentAt: DateTime
    abstract val taskName: TaskName

    @JsonIgnore fun getStateId() = taskName.name
}

data class TaskStatusUpdated constructor(
    override val sentAt: DateTime = DateTime(),
    override val taskName: TaskName,
    val taskId: TaskId,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus
) : TaskMetricMessage()

data class TaskMetricCreated(
    override val sentAt: DateTime = DateTime(),
    override val taskName: TaskName
) : TaskMetricMessage()
