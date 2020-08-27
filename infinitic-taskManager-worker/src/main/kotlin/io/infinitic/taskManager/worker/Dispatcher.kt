package io.infinitic.taskManager.worker

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {
    suspend fun toTaskEngine(msg: ForTaskEngineMessage) {
        avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg))
    }
}
