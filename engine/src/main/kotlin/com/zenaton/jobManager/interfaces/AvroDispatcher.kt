package com.zenaton.jobManager.interfaces

import com.zenaton.jobManager.messages.AvroForEngineMessage
import com.zenaton.jobManager.messages.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.AvroForWorkerMessage

interface AvroDispatcher {
    fun toWorkers(msg: AvroForWorkerMessage)
    fun toEngine(msg: AvroForEngineMessage, after: Float = 0f)
    fun toMonitoringGlobal(msg: AvroForMonitoringGlobalMessage)
    fun toMonitoringPerName(msg: AvroForMonitoringPerNameMessage)
}
