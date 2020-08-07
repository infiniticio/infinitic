package com.zenaton.jobManager.engine.avroClasses

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.engine.dispatcher.Dispatcher
import com.zenaton.jobManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.engine.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.engine.engines.MonitoringPerName
import com.zenaton.jobManager.engine.storages.MonitoringPerNameStorage
import org.slf4j.Logger

class AvroMonitoringPerName {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    private val monitoring = MonitoringPerName()

    fun handle(input: AvroEnvelopeForMonitoringPerName) {
        monitoring.logger = logger
        monitoring.storage = MonitoringPerNameStorage(avroStorage)
        monitoring.dispatcher = Dispatcher(avroDispatcher)

        monitoring.handle(AvroConverter.fromMonitoringPerName(input))
    }
}
