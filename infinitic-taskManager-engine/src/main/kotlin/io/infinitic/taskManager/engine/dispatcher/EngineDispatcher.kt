package io.infinitic.taskManager.engine.dispatcher

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage

class EngineDispatcher(private val avroDispatcher: AvroDispatcher) {

    suspend fun toTaskEngine(msg: ForTaskEngineMessage, after: Float = 0f) {
        avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg), after)
    }

    suspend fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        avroDispatcher.toMonitoringPerName(AvroConverter.toMonitoringPerName(msg))
    }

    suspend fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        avroDispatcher.toMonitoringGlobal(AvroConverter.toMonitoringGlobal(msg))
    }

    suspend fun toWorkers(msg: ForWorkerMessage) {
        avroDispatcher.toWorkers(AvroConverter.toWorkers(msg))
    }
}
