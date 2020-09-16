package io.infinitic.messaging.api.dispatcher.transport

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForWorker

interface AvroCompatibleTransport {
    suspend fun toWorkers(msg: AvroEnvelopeForWorker)
    suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine, after: Float = 0f)
    suspend fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal)
    suspend fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName)
}
