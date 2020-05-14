package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.commands.RetryTaskAttempt
import com.zenaton.taskmanager.messages.commands.TimeOutTaskAttempt
import com.zenaton.taskmanager.messages.events.TaskAttemptDispatched
import com.zenaton.taskmanager.messages.events.TaskAttemptTimedOut
import com.zenaton.workflowengine.topics.workflows.messages.TaskCompleted

interface TaskEngineDispatcherInterface {
    fun dispatch(msg: RunTask)
    fun dispatch(msg: TaskAttemptTimedOut)
    fun dispatch(msg: TaskCompleted)
    fun dispatch(msg: RetryTaskAttempt, after: Float = 0f)
    fun dispatch(msg: TimeOutTaskAttempt, after: Float = 0f)
    fun dispatch(msg: TaskAttemptDispatched)
}
