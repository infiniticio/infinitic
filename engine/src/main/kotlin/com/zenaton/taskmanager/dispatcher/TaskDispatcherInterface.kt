package com.zenaton.taskmanager.dispatcher

import com.zenaton.taskmanager.messages.RunTask
import com.zenaton.taskmanager.messages.TaskMessage
import com.zenaton.taskmanager.messages.TaskStatusUpdated

interface TaskDispatcherInterface {
    fun dispatch(msg: RunTask)
    fun dispatch(msg: TaskStatusUpdated)
    fun dispatch(msg: TaskMessage, after: Float = 0f)
}
