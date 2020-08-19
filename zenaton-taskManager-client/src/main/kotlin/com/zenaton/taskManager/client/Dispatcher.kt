package com.zenaton.taskManager.client

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.messages.ForJobEngineMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toJobEngine(msg: ForJobEngineMessage) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg))
    }
}
