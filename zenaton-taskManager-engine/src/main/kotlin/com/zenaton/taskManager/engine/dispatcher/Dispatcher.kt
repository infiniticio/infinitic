package com.zenaton.taskManager.engine.dispatcher

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.taskManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.taskManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.taskManager.common.messages.ForTaskEngineMessage
import com.zenaton.taskManager.common.messages.ForWorkerMessage

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
