package io.infinitic.taskManager.client

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage

class ClientDispatcher(private val avroDispatcher: AvroTaskDispatcher) {
    suspend fun toTaskEngine(msg: ForTaskEngineMessage) {
        avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg))
    }
}
