package com.zenaton.taskmanager.messages.events

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.messages.interfaces.TaskMessageInterface

data class TaskCanceled(
    override var taskId: TaskId,
    override var sentAt: DateTime? = DateTime()
) : TaskMessageInterface
