package com.zenaton.sdk

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.messages.ForJobEngineMessage
import com.zenaton.sdk.avro.AvroDispatcher

class Dispatcher(private val avroDispatcher: AvroDispatcher) {

    fun toJobEngine(msg: ForJobEngineMessage, after: Float = 0f) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg), after)
    }
}
