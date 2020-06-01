package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.engine.EngineMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.jobManager.workers.WorkerMessage

interface Dispatcher {
    fun dispatch(msg: WorkerMessage)
    fun dispatch(msg: MonitoringGlobalMessage)
    fun dispatch(msg: MonitoringPerNameMessage)
    fun dispatch(msg: EngineMessage, after: Float = 0f)
}
