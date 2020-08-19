package io.infinitic.taskManager.engine.avroInterfaces

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker

interface AvroDispatcher {
    fun toWorkers(msg: AvroEnvelopeForWorker)
    fun toTaskEngine(msg: AvroEnvelopeForTaskEngine, after: Float = 0f)
    fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal)
    fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName)
}
