package com.zenaton.sdk.avro

import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine

interface AvroDispatcher {
    fun toJobEngine(msg: AvroEnvelopeForJobEngine, after: Float = 0f)
}
