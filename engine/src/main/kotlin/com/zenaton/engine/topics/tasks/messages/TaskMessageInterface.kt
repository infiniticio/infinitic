package com.zenaton.engine.topics.tasks.messages

import com.zenaton.engine.data.DateTime
import com.zenaton.engine.data.tasks.TaskId

interface TaskMessageInterface {
    val taskId: TaskId
    var receivedAt: DateTime?
    fun getKey() = taskId.id
}
