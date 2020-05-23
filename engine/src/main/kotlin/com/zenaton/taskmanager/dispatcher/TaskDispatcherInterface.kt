package com.zenaton.taskmanager.dispatcher

import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.metrics.TaskStatusUpdated
import com.zenaton.taskmanager.messages.workers.RunTask

interface TaskDispatcherInterface {
    fun dispatch(msg: RunTask)
    fun dispatch(msg: TaskStatusUpdated)
    fun dispatch(msg: TaskEngineMessage, after: Float = 0f)
}
