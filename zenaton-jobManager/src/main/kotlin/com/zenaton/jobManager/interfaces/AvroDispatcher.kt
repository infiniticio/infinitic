package com.zenaton.jobManager.interfaces

import com.zenaton.jobManager.messages.envelopes.AvroForEngineMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessage
import com.zenaton.workflowManager.messages.envelopes.AvroForEngineMessage as AvroForWorkflowsMessage

interface AvroDispatcher {
    fun toWorkers(msg: AvroForWorkerMessage)
    fun toEngine(msg: AvroForEngineMessage, after: Float = 0f)
    fun toWorkflows(msg: AvroForWorkflowsMessage)
    fun toMonitoringGlobal(msg: AvroForMonitoringGlobalMessage)
    fun toMonitoringPerName(msg: AvroForMonitoringPerNameMessage)
}
