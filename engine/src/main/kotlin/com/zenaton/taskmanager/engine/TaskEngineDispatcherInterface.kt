package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.commands.CancelTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskCanceled
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted

interface TaskEngineDispatcherInterface {
    fun dispatch(msg: CancelTask)
    fun dispatch(msg: RunTask)
    fun dispatch(msg: TaskCompleted)
    fun dispatch(msg: RetryTaskAttempt, after: Float = 0f)
    fun dispatch(msg: TaskAttemptDispatched)
    fun dispatch(msg: TaskCanceled)
}
