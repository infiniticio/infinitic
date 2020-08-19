package com.zenaton.taskManager.engine.avroInterfaces

import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForWorker

interface AvroDispatcher {
    fun toWorkers(msg: AvroEnvelopeForWorker)
    fun toTaskEngine(msg: AvroEnvelopeForTaskEngine, after: Float = 0f)
    fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal)
    fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName)
}
