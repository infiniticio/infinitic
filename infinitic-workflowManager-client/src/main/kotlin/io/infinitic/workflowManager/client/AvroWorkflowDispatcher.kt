package io.infinitic.workflowManager.client

import io.infinitic.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine

interface AvroWorkflowDispatcher {
    suspend fun toWorkflowEngine(msg: AvroEnvelopeForWorkflowEngine)
}
