package com.zenaton.taskmanager.messages

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.commons.data.DateTime
import com.zenaton.taskmanager.data.TaskId

interface TaskMessageInterface {
    val taskId: TaskId
    val sentAt: DateTime
    @JsonIgnore fun getStateId() = taskId.id
}
