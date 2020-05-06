package com.zenaton.engine.taskAttempts.interfaces

import com.zenaton.engine.interfaces.MessageInterface
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.interfaces.data.NameInterface
import com.zenaton.engine.taskAttempts.data.TaskAttemptId
import com.zenaton.engine.tasks.data.TaskId

interface TaskAttemptMessageInterface : MessageInterface {
    val taskId: TaskId
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
    fun getName(): NameInterface
    override var sentAt: DateTime?
    override var receivedAt: DateTime?
    override fun getKey() = taskAttemptId.id
}
