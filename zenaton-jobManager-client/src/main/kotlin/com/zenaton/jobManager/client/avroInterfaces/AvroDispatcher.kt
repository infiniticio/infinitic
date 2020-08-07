package com.zenaton.jobManager.client.avroInterfaces

import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine

interface AvroDispatcher {
    fun toJobEngine(msg: AvroEnvelopeForJobEngine)
}
