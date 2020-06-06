package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.messages.interfaces.ForEngineMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.interfaces.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.interfaces.ForWorkerMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {

    fun toWorkers(msg: ForWorkerMessage) {
        avroDispatcher.toWorkers(AvroConverter.toWorkers(msg))
    }

    fun toEngine(msg: ForEngineMessage, after: Float = 0f) {
        avroDispatcher.toEngine(AvroConverter.toEngine(msg))
    }

    fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        avroDispatcher.toMonitoringGlobal(AvroConverter.toMonitoringGlobal(msg))
    }

    fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        avroDispatcher.toMonitoringPerName(AvroConverter.toMonitoringPerName(msg))
    }
}
