package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.messages.ForJobEngineMessage
import com.zenaton.jobManager.messages.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.ForWorkerMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {

    fun toWorkers(msg: ForWorkerMessage) {
        avroDispatcher.toWorkers(AvroConverter.toWorkers(msg))
    }

    fun toJobEngine(msg: ForJobEngineMessage, after: Float = 0f) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg), after)
    }

    fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        avroDispatcher.toMonitoringGlobal(AvroConverter.toMonitoringGlobal(msg))
    }

    fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        avroDispatcher.toMonitoringPerName(AvroConverter.toMonitoringPerName(msg))
    }
}
