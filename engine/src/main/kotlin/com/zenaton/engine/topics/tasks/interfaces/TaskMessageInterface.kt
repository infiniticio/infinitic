package com.zenaton.engine.topics.tasks.interfaces

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.interfaces.MessageInterface

interface TaskMessageInterface : MessageInterface {
    val taskId: TaskId
    @JsonIgnore override fun getKey() = taskId.id
}
