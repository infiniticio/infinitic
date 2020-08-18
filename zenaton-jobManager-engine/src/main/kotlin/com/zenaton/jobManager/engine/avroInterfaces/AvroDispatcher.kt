package com.zenaton.jobManager.engine.avroInterfaces

import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker

interface AvroDispatcher {
    fun toWorkers(msg: AvroEnvelopeForWorker)
    fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float = 0f)
    fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal)
    fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName)
}
