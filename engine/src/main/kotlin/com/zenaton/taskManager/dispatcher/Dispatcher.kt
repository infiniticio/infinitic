package com.zenaton.taskManager.dispatcher

import com.zenaton.taskManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.taskManager.engine.EngineMessage
import com.zenaton.taskManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.taskManager.workers.WorkerMessage

interface Dispatcher {
    fun dispatch(msg: WorkerMessage)
    fun dispatch(msg: MonitoringGlobalMessage)
    fun dispatch(msg: MonitoringPerNameMessage)
    fun dispatch(msg: EngineMessage, after: Float = 0f)
}
