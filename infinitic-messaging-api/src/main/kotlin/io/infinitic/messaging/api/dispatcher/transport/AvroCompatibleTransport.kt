package io.infinitic.messaging.api.dispatcher.transport

import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.avro.taskManager.messages.envelopes.AvroEnvelopeForWorker
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine

interface AvroCompatibleTransport {
    suspend fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float = 0f)
    suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine, after: Float = 0f)
    suspend fun toMonitoringGlobal(msg: AvroEnvelopeForMonitoringGlobal)
    suspend fun toMonitoringPerName(msg: AvroEnvelopeForMonitoringPerName)
    suspend fun toWorkers(msg: AvroEnvelopeForWorker)
}
