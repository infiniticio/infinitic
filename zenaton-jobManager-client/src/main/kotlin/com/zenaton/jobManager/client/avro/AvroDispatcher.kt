package com.zenaton.jobManager.client.avro

import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine

interface AvroDispatcher {
    fun toJobEngine(msg: AvroEnvelopeForJobEngine)
}
