package com.zenaton.taskmanager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId

interface TaskMessage {
    val taskId: TaskId
    val sentAt: DateTime
}
