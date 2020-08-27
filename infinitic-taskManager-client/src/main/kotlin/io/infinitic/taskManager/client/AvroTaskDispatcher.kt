package io.infinitic.taskManager.client

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine

interface AvroTaskDispatcher {
    suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine)
}
