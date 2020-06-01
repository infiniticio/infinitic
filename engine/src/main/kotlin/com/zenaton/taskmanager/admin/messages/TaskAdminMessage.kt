package com.zenaton.taskmanager.admin.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskName

sealed class TaskAdminMessage {
    abstract val sentAt: DateTime
    abstract val taskName: TaskName
}

data class TaskTypeCreated(
    override val sentAt: DateTime = DateTime(),
    override val taskName: TaskName
) : TaskAdminMessage()
