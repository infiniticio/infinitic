package io.infinitic.taskManager.engine.dispatcher

import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.engine.avroInterfaces.AvroDispatcher
import io.infinitic.taskManager.common.messages.ForMonitoringGlobalMessage
import io.infinitic.taskManager.common.messages.ForMonitoringPerNameMessage
import io.infinitic.taskManager.common.messages.ForTaskEngineMessage
import io.infinitic.taskManager.common.messages.ForWorkerMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {

    fun toTaskEngine(msg: ForTaskEngineMessage, after: Float = 0f) {
        avroDispatcher.toTaskEngine(AvroConverter.toTaskEngine(msg), after)
    }

    fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        avroDispatcher.toMonitoringPerName(AvroConverter.toMonitoringPerName(msg))
    }

    fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        avroDispatcher.toMonitoringGlobal(AvroConverter.toMonitoringGlobal(msg))
    }

    fun toWorkers(msg: ForWorkerMessage) {
        avroDispatcher.toWorkers(AvroConverter.toWorkers(msg))
    }
}
