package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.engine.EngineMessage
import com.zenaton.jobManager.monitoring.global.MonitoringGlobalMessage
import com.zenaton.jobManager.monitoring.perInstance.MonitoringPerInstanceMessage
import com.zenaton.jobManager.monitoring.perName.MonitoringPerNameMessage
import com.zenaton.jobManager.workers.WorkerMessage

interface Dispatcher {
    fun toWorkers(msg: WorkerMessage)
    fun toEngine(msg: EngineMessage, after: Float = 0f)
    fun toMonitoringPerInstance(msg: MonitoringPerInstanceMessage)
    fun toMonitoringPerInstance(msg: EngineMessage)
    fun toMonitoringGlobal(msg: MonitoringGlobalMessage)
    fun toMonitoringPerName(msg: MonitoringPerNameMessage)
}
