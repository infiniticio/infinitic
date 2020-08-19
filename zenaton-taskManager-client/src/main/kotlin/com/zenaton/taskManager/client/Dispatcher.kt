package com.zenaton.taskManager.client

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.common.messages.ForTaskEngineMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toTaskEngine(msg: ForTaskEngineMessage) {
        avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg))
    }
}
