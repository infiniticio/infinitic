package com.zenaton.taskManager.messages

import com.zenaton.commons.data.DateTime
import com.zenaton.taskManager.data.TaskId

interface TaskMessage {
    val taskId: TaskId
    val sentAt: DateTime
}
