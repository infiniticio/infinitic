package io.infinitic.taskManager.client

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    fun toTaskEngine(msg: ForTaskEngineMessage) {
        avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg))
    }
}
