package io.infinitic.taskManager.worker

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine

interface AvroDispatcher {
    suspend fun toTaskEngine(msg: AvroEnvelopeForTaskEngine)
}
