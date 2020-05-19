package com.zenaton.taskmanager.messages.commands

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.messages.TaskMessageInterface

data class CancelTask(
    override val taskId: TaskId,
    override val sentAt: DateTime = DateTime()
) : TaskMessageInterface
