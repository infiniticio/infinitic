package com.zenaton.engine.taskAttempts.messages

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.interfaces.messages.MessageInterface
import com.zenaton.engine.taskAttempts.data.TaskAttemptId
import com.zenaton.engine.tasks.data.TaskId

interface TaskAttemptInterface : MessageInterface {
    val taskId: TaskId
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
    override var sentAt: DateTime?
    override var receivedAt: DateTime?
    override fun getKey() = taskAttemptId.id
}
