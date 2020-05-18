package com.zenaton.taskmanager.messages.commands

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.messages.TaskMessageInterface

data class RetryTask(
    override var taskId: TaskId,
    override var sentAt: DateTime = DateTime()
) : TaskMessageInterface
