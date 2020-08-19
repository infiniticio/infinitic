package com.zenaton.taskManager.engine.avroClasses

import com.zenaton.taskManager.common.avro.AvroConverter
import com.zenaton.taskManager.engine.dispatcher.Dispatcher
import com.zenaton.taskManager.engine.avroInterfaces.AvroDispatcher
import com.zenaton.taskManager.engine.avroInterfaces.AvroStorage
import com.zenaton.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.taskManager.engine.engines.MonitoringPerName
import com.zenaton.taskManager.engine.storages.MonitoringPerNameStorage
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
