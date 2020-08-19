package io.infinitic.taskManager.worker

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine

interface AvroDispatcher {
    fun toTaskEngine(msg: AvroEnvelopeForTaskEngine)
}
