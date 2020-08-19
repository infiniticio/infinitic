package io.infinitic.workflowManager.engine.avroInterfaces

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine

interface AvroDispatcher {
    fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float = 0f)
    fun toDeciders(msg: AvroEnvelopeForTaskEngine)
    fun toWorkers(msg: AvroEnvelopeForTaskEngine)
}
