package com.zenaton.taskmanager.dispatcher

import com.zenaton.taskmanager.messages.TaskMessageInterface
import com.zenaton.taskmanager.messages.commands.RunTask
import com.zenaton.taskmanager.messages.events.TaskStatusUpdated

interface TaskDispatcherInterface {
    fun dispatch(msg: RunTask)
    fun dispatch(msg: TaskStatusUpdated)
    fun dispatch(msg: TaskMessageInterface, after: Float = 0f)
}
