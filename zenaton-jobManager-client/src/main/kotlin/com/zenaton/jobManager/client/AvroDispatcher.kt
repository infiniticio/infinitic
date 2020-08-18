package com.zenaton.jobManager.client

import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine

interface AvroDispatcher {
    fun toJobEngine(msg: AvroEnvelopeForJobEngine)
}
