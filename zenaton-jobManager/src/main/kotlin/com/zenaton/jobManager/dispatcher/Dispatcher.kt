package com.zenaton.jobManager.dispatcher

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.messages.envelopes.ForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkerMessage

class Dispatcher(private val avroDispatcher: AvroDispatcher) {

    fun toWorkers(msg: ForWorkerMessage) {
        avroDispatcher.toWorkers(AvroConverter.toWorkers(msg))
    }

    fun toJobEngine(msg: ForJobEngineMessage, after: Float = 0f) {
        avroDispatcher.toJobEngine(AvroConverter.toJobEngine(msg))
    }

    fun toMonitoringGlobal(msg: ForMonitoringGlobalMessage) {
        avroDispatcher.toMonitoringGlobal(AvroConverter.toMonitoringGlobal(msg))
    }

    fun toMonitoringPerName(msg: ForMonitoringPerNameMessage) {
        avroDispatcher.toMonitoringPerName(AvroConverter.toMonitoringPerName(msg))
    }
}
