package com.zenaton.taskmanager.messages.interfaces

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.engine.data.DateTime
import com.zenaton.taskmanager.data.TaskId

interface TaskMessageInterface {
    val taskId: TaskId
    var sentAt: DateTime?
    @JsonIgnore fun getStateId() = taskId.id
}
