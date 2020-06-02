package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerInstanceMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage

interface Dispatcher {
    fun toWorkers(msg: ForWorkerMessage)
    fun toEngine(msg: ForEngineMessage, after: Float = 0f)
    fun toMonitoringPerInstance(msg: ForMonitoringPerInstanceMessage)
    fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage)
    fun toMonitoringPerName(msg: ForMonitoringPerNameMessage)
}
