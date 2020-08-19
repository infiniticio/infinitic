package com.zenaton.taskManager.client

import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForJobEngine

interface AvroDispatcher {
    fun toJobEngine(msg: AvroEnvelopeForJobEngine)
}
