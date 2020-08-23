package io.infinitic.taskManager.client

import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine

interface AvroTaskDispatcher {
    fun toTaskEngine(msg: AvroEnvelopeForTaskEngine)
}
