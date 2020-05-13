package com.zenaton.taskmanager.engine

import com.zenaton.engine.topics.workflows.messages.TaskCompleted
import com.zenaton.taskmanager.messages.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.TaskAttemptRetried
import com.zenaton.taskmanager.messages.TaskAttemptTimeout

interface TaskEngineDispatcherInterface {
    fun dispatch(msg: TaskCompleted)
    fun dispatch(msg: TaskAttemptRetried, after: Float = 0f)
    fun dispatch(msg: TaskAttemptTimeout, after: Float = 0f)
    fun dispatch(msg: TaskAttemptDispatched)
}
