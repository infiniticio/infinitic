package com.zenaton.jobManager.interfaces

import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessage
import com.zenaton.workflowManager.messages.envelopes.AvroForWorkflowEngineMessage

interface AvroDispatcher {
    fun toWorkers(msg: AvroForWorkerMessage)
    fun toJobEngine(msg: AvroForJobEngineMessage, after: Float = 0f)
    fun toWorkflowEngine(msg: AvroForWorkflowEngineMessage)
    fun toMonitoringGlobal(msg: AvroForMonitoringGlobalMessage)
    fun toMonitoringPerName(msg: AvroForMonitoringPerNameMessage)
}
