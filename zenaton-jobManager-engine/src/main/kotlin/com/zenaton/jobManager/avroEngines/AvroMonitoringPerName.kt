package com.zenaton.jobManager.avroEngines

import com.zenaton.jobManager.common.avro.AvroConverter
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.avroInterfaces.AvroDispatcher
import com.zenaton.jobManager.avroInterfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.engines.MonitoringPerName
import com.zenaton.jobManager.storages.MonitoringPerNameStorage
import org.slf4j.Logger

class AvroMonitoringPerName {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    val monitoring = MonitoringPerName()

    fun handle(input: AvroEnvelopeForMonitoringPerName) {
        monitoring.logger = logger
        monitoring.storage = MonitoringPerNameStorage(avroStorage)
        monitoring.dispatcher = Dispatcher(avroDispatcher)

        monitoring.handle(AvroConverter.fromMonitoringPerName(input))
    }
}
