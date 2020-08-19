package com.zenaton.workflowManager.avroInterfaces

import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine
import com.zenaton.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine

interface AvroDispatcher {
    fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine, after: Float = 0f)
    fun toDeciders(msg: AvroEnvelopeForTaskEngine)
    fun toWorkers(msg: AvroEnvelopeForTaskEngine)
}
