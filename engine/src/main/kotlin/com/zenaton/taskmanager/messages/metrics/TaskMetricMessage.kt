package com.zenaton.taskmanager.messages.metrics

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.messages.interfaces.TaskMessage

sealed class TaskMetricMessage(
    override val taskId: TaskId,
    override val sentAt: DateTime,
    open val taskName: TaskName
) : TaskMessage {
    @JsonIgnore fun getStateId() = taskName.name
}

data class TaskStatusUpdated(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime(),
    override val taskName: TaskName,
    val oldStatus: TaskStatus?,
    val newStatus: TaskStatus
) : TaskMetricMessage(taskId, sentAt, taskName)
