package com.zenaton.taskmanager.messages.events

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskAttemptId
import com.zenaton.taskmanager.data.TaskId
import com.zenaton.taskmanager.data.TaskOutput
import com.zenaton.taskmanager.messages.TaskAttemptMessageInterface

data class TaskAttemptCompleted(
    override var taskId: TaskId,
    override var sentAt: DateTime = DateTime(),
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptIndex: Int,
    val taskOutput: TaskOutput?
) : TaskAttemptMessageInterface
