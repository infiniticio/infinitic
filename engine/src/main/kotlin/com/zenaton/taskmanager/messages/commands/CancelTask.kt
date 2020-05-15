package com.zenaton.taskmanager.messages.commands

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface

data class CancelTask(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime()
) : TaskMessageInterface
