package com.zenaton.jobManager.client

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.messages.ForJobEngineMessage
import com.zenaton.jobManager.client.avro.AvroDispatcher

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toJobEngine(msg: ForJobEngineMessage) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg))
    }
}
