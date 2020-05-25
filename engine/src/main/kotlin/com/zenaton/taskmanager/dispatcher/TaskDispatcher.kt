package com.zenaton.taskmanager.dispatcher

import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.workers.TaskWorkerMessage

interface TaskDispatcher {
    fun dispatch(msg: TaskWorkerMessage)
    fun dispatch(msg: TaskMetricMessage)
    fun dispatch(msg: TaskEngineMessage, after: Float = 0f)
}
