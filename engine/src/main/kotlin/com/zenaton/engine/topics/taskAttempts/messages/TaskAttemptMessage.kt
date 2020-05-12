package com.zenaton.engine.topics.taskAttempts.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.TaskAttemptId
import com.zenaton.engine.data.TaskData
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.TaskName

data class TaskAttemptMessage(
    var taskId: TaskId,
    val taskAttemptId: TaskAttemptId,
    val taskAttemptIndex: Int,
    var sentAt: DateTime? = DateTime(),
    val taskName: TaskName,
    val taskData: TaskData?
)
