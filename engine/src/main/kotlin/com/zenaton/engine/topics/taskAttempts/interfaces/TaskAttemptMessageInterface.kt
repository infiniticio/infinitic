package com.zenaton.engine.topics.taskAttempts.interfaces

import com.zenaton.engine.data.TaskAttemptId
import com.zenaton.engine.data.TaskId
import com.zenaton.engine.data.interfaces.NameInterface
import com.zenaton.engine.interfaces.MessageInterface

interface TaskAttemptMessageInterface : MessageInterface {
    val taskId: TaskId
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
    fun getName(): NameInterface
    override fun getKey() = taskAttemptId.id
}
