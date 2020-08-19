package com.zenaton.taskManager.engine.dispatcher

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.taskManager.common.messages.ForJobEngineMessage
import com.zenaton.taskManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.taskManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.taskManager.common.messages.ForWorkerMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {

    fun toJobEngine(msg: ForJobEngineMessage, after: Float = 0f) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg), after)
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
