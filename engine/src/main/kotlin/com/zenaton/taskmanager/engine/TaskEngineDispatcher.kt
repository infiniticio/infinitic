package com.zenaton.taskmanager.engine

import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.workers.TaskWorkerMessage

interface TaskEngineDispatcher {
    fun dispatch(msg: TaskWorkerMessage)
    fun dispatch(msg: TaskMetricMessage)
    fun dispatch(msg: TaskEngineMessage, after: Float = 0f)
}
