package com.zenaton.engine.tasks.engine

import com.zenaton.engine.taskAttempts.messages.TaskAttemptDispatched
import com.zenaton.engine.workflows.messages.TaskCompleted

interface TaskEngineDispatcherInterface {
    fun dispatch(msg: TaskAttemptDispatched, after: Float = 0f)
    fun dispatch(msg: TaskCompleted, after: Float = 0f)
}
