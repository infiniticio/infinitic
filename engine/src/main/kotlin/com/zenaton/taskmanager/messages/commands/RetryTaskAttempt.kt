package com.zenaton.taskmanager.messages.commands

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.messages.TaskAttemptMessageInterface

data class RetryTaskAttempt(
    override var taskId: TaskId,
    override var sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int
) : TaskAttemptMessageInterface
