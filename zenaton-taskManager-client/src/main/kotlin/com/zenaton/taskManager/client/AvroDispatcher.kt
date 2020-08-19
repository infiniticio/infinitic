package com.zenaton.taskManager.client

import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine

interface AvroDispatcher {
    fun toTaskEngine(msg: AvroEnvelopeForTaskEngine)
}
