package com.zenaton.jobManager.engine.dispatcher

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.common.messages.ForJobEngineMessage
import com.zenaton.jobManager.common.messages.ForMonitoringGlobalMessage
import com.zenaton.jobManager.common.messages.ForMonitoringPerNameMessage
import com.zenaton.jobManager.common.messages.ForWorkerMessage

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
