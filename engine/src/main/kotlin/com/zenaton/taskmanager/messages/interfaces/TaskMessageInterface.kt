package com.zenaton.taskmanager.messages.interfaces

import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId

interface TaskMessageInterface {
    val taskId: TaskId
    val sentAt: DateTime
}
