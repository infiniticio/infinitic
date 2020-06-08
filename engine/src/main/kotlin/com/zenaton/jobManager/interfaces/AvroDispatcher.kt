package com.zenaton.jobManager.interfaces

import com.zenaton.jobManager.messages.envelopes.AvroForEngineMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessage

interface AvroDispatcher {
    fun toWorkers(msg: AvroForWorkerMessage)
    fun toEngine(msg: AvroForEngineMessage, after: Float = 0f)
    fun toMonitoringGlobal(msg: AvroForMonitoringGlobalMessage)
    fun toMonitoringPerName(msg: AvroForMonitoringPerNameMessage)
}
