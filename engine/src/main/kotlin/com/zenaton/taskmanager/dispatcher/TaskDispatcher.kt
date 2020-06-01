package com.zenaton.taskmanager.dispatcher

import com.zenaton.taskmanager.admin.messages.TaskAdminMessage
import com.zenaton.taskmanager.engine.messages.TaskEngineMessage
import com.zenaton.taskmanager.metrics.messages.TaskMetricMessage
import com.zenaton.taskmanager.workers.messages.TaskWorkerMessage

interface TaskDispatcher {
    fun dispatch(msg: TaskWorkerMessage)
    fun dispatch(msg: TaskAdminMessage)
    fun dispatch(msg: TaskMetricMessage)
    fun dispatch(msg: TaskEngineMessage, after: Float = 0f)
}
