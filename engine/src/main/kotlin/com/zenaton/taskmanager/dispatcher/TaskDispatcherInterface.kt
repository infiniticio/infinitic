package com.zenaton.taskmanager.dispatcher

import com.zenaton.taskmanager.messages.TaskMessageInterface

interface TaskDispatcherInterface {
    fun dispatch(msg: TaskMessageInterface, after: Float = 0f)
}
