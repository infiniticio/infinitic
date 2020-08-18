package com.zenaton.jobManager.worker

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.common.messages.ForJobEngineMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toJobEngine(msg: ForJobEngineMessage) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg))
    }
}
