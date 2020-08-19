package com.zenaton.taskManager.worker

import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForTaskEngine

interface AvroDispatcher {
    fun toTaskEngine(msg: AvroEnvelopeForTaskEngine)
}
