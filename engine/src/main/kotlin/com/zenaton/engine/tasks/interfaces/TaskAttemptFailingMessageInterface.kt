package com.zenaton.engine.tasks.interfaces

import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.taskAttempts.data.TaskAttemptId
import com.zenaton.engine.tasks.data.TaskId

interface TaskAttemptFailingMessageInterface :
    TaskMessageInterface {
    override val taskId: TaskId
    override var sentAt: DateTime?
    override var receivedAt: DateTime?
    override fun getKey() = taskId.id
    val taskAttemptId: TaskAttemptId
    val taskAttemptIndex: Int
    val taskAttemptDelayBeforeRetry: Float?
}
