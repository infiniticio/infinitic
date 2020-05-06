package com.zenaton.engine.tasks.interfaces

import com.zenaton.engine.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.engine.tasks.messages.TaskAttemptRetried
import com.zenaton.engine.tasks.messages.TaskAttemptTimeout
import com.zenaton.engine.workflows.messages.TaskCompleted

interface TaskEngineDispatcherInterface {
    fun dispatch(msg: TaskCompleted)
    fun dispatch(msg: TaskAttemptRetried, after: Float = 0f)
    fun dispatch(msg: TaskAttemptTimeout, after: Float = 0f)
    fun dispatch(msg: TaskAttemptDispatched)
}
