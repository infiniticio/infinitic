package io.infinitic.taskManager.client

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine

interface AvroDispatcher {
    fun toTaskEngine(msg: AvroEnvelopeForTaskEngine)
}
