package io.infinitic.workflowManager.engine.avroInterfaces

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine

interface AvroDispatcher {
    suspend fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float = 0f)
    suspend fun toDeciders(msg: AvroEnvelopeForTaskEngine)
    suspend fun toWorkers(msg: AvroEnvelopeForTaskEngine)
}
