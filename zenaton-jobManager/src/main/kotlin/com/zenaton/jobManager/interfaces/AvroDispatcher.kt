package com.zenaton.jobManager.interfaces

import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine

interface AvroDispatcher {
    fun toWorkers(msg: AvroEnvelopeForWorker)
    fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float = 0f)
    fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine)
    fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal)
    fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName)
}
