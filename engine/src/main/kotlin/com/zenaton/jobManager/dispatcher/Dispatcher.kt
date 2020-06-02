package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.messages.interfaces.EngineMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.MonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.WorkerMessage

interface Dispatcher {
    fun toWorkers(msg: WorkerMessage)
    fun toEngine(msg: EngineMessage, after: Float = 0f)
    fun toMonitoringPerInstance(msg: MonitoringPerInstanceMessage)
    fun toMonitoringGlobal(msg: MonitoringGlobalMessage)
    fun toMonitoringPerName(msg: MonitoringPerNameMessage)
}
