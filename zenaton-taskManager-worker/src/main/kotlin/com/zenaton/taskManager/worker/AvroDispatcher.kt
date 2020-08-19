package com.zenaton.taskManager.worker

import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForJobEngine

interface AvroDispatcher {
    fun toJobEngine(msg: AvroEnvelopeForJobEngine)
}
