package com.zenaton.jobManager.client

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.messages.ForJobEngineMessage
import com.zenaton.jobManager.client.avro.AvroDispatcher

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toJobEngine(msg: ForJobEngineMessage) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg))
    }
}
